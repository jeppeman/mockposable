package com.jeppeman.mockposable.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.validateIr
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.toLogger
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.Logger

/**
 * This extension has the responsibility of performing the following transformations:
 *
 * 1. everyComposable { f(args, $composer, $changed) } -> everyComposable { f(args, any<Composer?>(), any<Int>() }
 * 2. verifyComposable { f(args, $composer, $changed) } -> verifyComposable { f(args, any<Composer?>(), any<Int>() }
 *
 * The reason for this is that the composable functions get called with different instances of
 * androidx.compose.runtime.Composer for different calls. Transforming to any() makes it so we can
 * verify these calls with Mockk.
 */
class MockKIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val logger: Logger = messageCollector.toLogger(),
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        logger.log("Running MockK composable transformations")
        val transformers = listOf(
            EveryComposableElementTransformer(logger, pluginContext),
            VerifyComposableElementTransformer(logger, pluginContext)
        )
        transformers.forEach { transformer -> moduleFragment.transform(transformer, null) }
        validateIr(messageCollector, IrVerificationMode.ERROR) {
            performBasicIrValidation(
                moduleFragment,
                pluginContext.irBuiltIns,
                "MockK transformation",
                IrValidatorConfig(),
            )
        }
    }
}

private abstract class MockKCallTransformer(
    protected val logger: Logger,
    protected val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    // We're after the stubBlock in everyComposable(stubBlock = @Composable { ... }) for example.
    abstract val composableBlockParameterName: String
    abstract fun transformPredicate(expression: IrCall): Boolean

    override fun visitCall(expression: IrCall): IrExpression {
        if (!transformPredicate(expression)) return super.visitCall(expression)

        val composableBlock = expression
            .extractComposableCallFromBlockArg(composableBlockParameterName)

        val mockKMatcherScope = composableBlock.function.extensionReceiverParameter
            ?: pluginError(
                "Expected an extensionReceiverParameter for function ${composableBlock.function.dumpKotlinLike()}, but was null."
            )

        val anyMatcherFunction = mockKMatcherScope.anyMatcherFunction

        composableBlock.transformAllComposableCalls(
            mockKMatcherScope, anyMatcherFunction, pluginContext, logger
        )

        return super.visitCall(expression)
    }
}

private class EveryComposableElementTransformer(
    logger: Logger,
    pluginContext: IrPluginContext
) : MockKCallTransformer(logger, pluginContext) {
    override val composableBlockParameterName: String = "stubBlock"

    override fun transformPredicate(
        expression: IrCall
    ): Boolean = expression.isEveryComposable && expression.isMockKStubScopeReturnType
}

private class VerifyComposableElementTransformer(
    logger: Logger,
    pluginContext: IrPluginContext
) : MockKCallTransformer(logger, pluginContext) {
    override val composableBlockParameterName: String = "verifyBlock"

    override fun transformPredicate(
        expression: IrCall
    ): Boolean = expression.isVerifyComposable
            || expression.isVerifyComposableAll
            || expression.isVerifyComposableOrder
            || expression.isVerifyComposableSequence
}

private fun IrFunctionExpression.transformAllComposableCalls(
    mockKScope: IrValueParameter,
    anyMatcherFunction: IrSimpleFunctionSymbol,
    pluginContext: IrPluginContext,
    logger: Logger
) {
    function.body?.findComposableCalls()?.forEach { composableCall ->
        composableCall.transformComposeArgs(
            pluginContext,
            // Transforming $composer to any<Composer?>()
            { composerValueArgument ->
                irCall(anyMatcherFunction).apply {
                    dispatchReceiver = irGet(mockKScope)
                    putTypeArgument(0, composerValueArgument.type)
                }
            },
            // Transforming $changed to any<Int>()
            { changedValueArgument ->
                irCall(anyMatcherFunction).apply {
                    dispatchReceiver = irGet(mockKScope)
                    putTypeArgument(0, changedValueArgument.type)
                }
            },
            logger
        )
    }
}

private val IrValueParameter.anyMatcherFunction: IrSimpleFunctionSymbol
    get() = type.classOrNull?.getSimpleFunction("any") ?: pluginError(
        "Failed to find the any() function on ${type.classFqName?.asString()}."
    )

private val everyComposableFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKK_PACKAGE_NAME}.everyComposable")

private val verifyComposableFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKK_PACKAGE_NAME}.verifyComposable")

private val verifyComposableAllFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKK_PACKAGE_NAME}.verifyComposableAll")

private val verifyComposableOrderFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKK_PACKAGE_NAME}.verifyComposableOrder")

private val verifyComposableSequenceFqName: FqName
    get() = FqName("${MOCKPOSABLE_MOCKK_PACKAGE_NAME}.verifyComposableSequence")

private val IrCall.isEveryComposable: Boolean
    get() = symbol.owner.fqNameWhenAvailable == everyComposableFqName

private val IrCall.isVerifyComposable: Boolean
    get() = symbol.owner.fqNameWhenAvailable == verifyComposableFqName

private val IrCall.isVerifyComposableAll: Boolean
    get() = symbol.owner.fqNameWhenAvailable == verifyComposableAllFqName

private val IrCall.isVerifyComposableOrder: Boolean
    get() = symbol.owner.fqNameWhenAvailable == verifyComposableOrderFqName

private val IrCall.isVerifyComposableSequence: Boolean
    get() = symbol.owner.fqNameWhenAvailable == verifyComposableSequenceFqName

private val mockKStubScopeFqName: FqName
    get() = FqName("${MOCKK_PACKAGE_NAME}.MockKStubScope")

private val IrCall.isMockKStubScopeReturnType: Boolean
    get() = type.classFqName == mockKStubScopeFqName

private const val MOCKK_PACKAGE_NAME = "io.mockk"
private const val MOCKPOSABLE_MOCKK_PACKAGE_NAME = "com.jeppeman.mockposable.mockk"
