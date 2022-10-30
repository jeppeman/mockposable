package com.jeppeman.mockposable.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.Logger

/**
 * This extension has the responsibility of performing the following transformations:
 *
 * 1. onComposable { f(args, $composer, $changed) } -> onComposable { f(args, any<Composer?>($composer), any<Int>($changed) }
 * 2. verifyComposable { f(args, $composer, $changed) } -> verifyComposable { f(args, any<Composer?>($composer), any<Int>($changed) }
 *
 * The reason for this is that the composable functions get called with different instances of
 * androidx.compose.runtime.Composer for different calls. Transforming to any() makes it so we
 * verify these calls with Mockito.
 */
class MockitoIrGenerationExtension(
    private val logger: Logger
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        logger.log("Running Mockito composable transformations")
        val transformers = listOf(
            OnComposableElementTransformer(logger, pluginContext),
            MockitoVerifyComposableElementTransformer(logger, pluginContext)
        )
        transformers.forEach { transformer -> moduleFragment.transform(transformer, null) }
        moduleFragment.checkDeclarationParents()
    }
}

private abstract class MockitoCallTransformer(
    protected val logger: Logger,
    protected val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    // We're after the stubBlock in everyComposable(stubBlock = @Composable { ... }) for example.
    abstract val composableBlockParameterName: String
    abstract fun transformPredicate(expression: IrCall):  Boolean

    override fun visitCall(expression: IrCall): IrExpression {
        if (!transformPredicate(expression)) return super.visitCall(expression)

        expression.extractComposableCallFromBlockArg(composableBlockParameterName)
            .transformAllComposableCalls(pluginContext, logger)

        return super.visitCall(expression)
    }
}

private class OnComposableElementTransformer(
    logger: Logger,
    pluginContext: IrPluginContext
) : MockitoCallTransformer(logger, pluginContext) {
    override val composableBlockParameterName: String = "stubBlock"

    override fun transformPredicate(
        expression: IrCall
    ): Boolean = expression.isOnComposable
}

private class MockitoVerifyComposableElementTransformer(
    logger: Logger,
    pluginContext: IrPluginContext
) : MockitoCallTransformer(logger, pluginContext) {
    override val composableBlockParameterName: String = "verifyBlock"

    override fun transformPredicate(
        expression: IrCall
    ): Boolean = expression.isVerifyComposable
}

private fun IrFunctionExpression.transformAllComposableCalls(
    pluginContext: IrPluginContext,
    logger: Logger
) {
    val anyMatcherFunction = pluginContext.anyMatcherFunction

    function.body?.findComposableCalls()?.forEach { composableCall ->
        composableCall.transformComposeArgs(
            pluginContext,
            // Transforming $composer to any<Composer?>($composer)
            { composerValueArgument ->
                irCall(anyMatcherFunction).apply {
                    putTypeArgument(0, composerValueArgument.type)
                    putValueArgument(0, composerValueArgument)
                }
            },
            // Transforming $composer to any<Int>($changed)
            { changedValueArgument ->
                irCall(anyMatcherFunction).apply {
                    putTypeArgument(0, changedValueArgument.type)
                    putValueArgument(0, changedValueArgument)
                }
            },
            logger
        )
    }
}

private val onComposableFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKITO_FILE_NAME}.onComposable")

private val verifyComposableFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKITO_FILE_NAME}.verifyComposable")

private val IrCall.isOnComposable: Boolean
    get() = symbol.owner.fqNameWhenAvailable == onComposableFqName

private val IrCall.isVerifyComposable: Boolean
    get() = symbol.owner.fqNameWhenAvailable == verifyComposableFqName

private val IrPluginContext.anyMatcherFunction: IrSimpleFunctionSymbol
    get() = referenceFunctions(FqName("${MOCKPOSABLE_MOCKITO_PACKAGE_NAME}.any"))
        .firstOrNull()
        ?: pluginError(
            "\"${MOCKPOSABLE_MOCKITO_PACKAGE_NAME}.any\" not found, this should not happen."
        )

private const val MOCKPOSABLE_MOCKITO_PACKAGE_NAME = "com.jeppeman.mockposable.mockito"
private const val MOCKPOSABLE_MOCKITO_FILE_NAME =
    "${MOCKPOSABLE_MOCKITO_PACKAGE_NAME}.MockposableMockitoKt"
