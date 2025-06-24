@file:OptIn(DeprecatedForRemovalCompilerApi::class)

package com.jeppeman.mockposable.compiler

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.Logger

fun FqName.classId(
    isLocal: Boolean = false
): ClassId = ClassId(parent(), FqName.topLevel(shortName()), isLocal)

fun IrPluginContext.classSymbol(
    fqName: String
): IrClassSymbol = referenceClass(FqName(fqName).classId())!!

fun String.callableId(
    packageName: FqName
): CallableId = CallableId(packageName, Name.identifier(this))

fun <T> IrPluginContext.buildIr(
    declarationSymbol: IrSymbol,
    block: DeclarationIrBuilder.() -> T
): T = DeclarationIrBuilder(this, declarationSymbol).run { block() }

val composableAnnotationFqName: FqName
    get() = FqName("${COMPOSE_RUNTIME_PACKAGE_NAME}.Composable")

val IrPluginContext.composerClassSymbol: IrClassSymbol
    get() = classSymbol("${COMPOSE_RUNTIME_PACKAGE_NAME}.Composer")

val IrCall.isComposableFunctionCall: Boolean
    get() = symbol.owner.hasAnnotation(composableAnnotationFqName)

val IrWhen.calls get() = branches.flatMap { branch -> branch.result.calls }

val IrContainerExpression.calls get() = statements.flatMap { statement -> statement.calls }

val IrTry.calls: List<IrCall> get() = allStatements().flatMap { (statement) -> statement.calls }

val IrLoop.calls: List<IrCall>
    get() = body?.allStatements()?.flatMap { (statement) -> statement.calls }.orEmpty()

val IrBody.calls: List<IrCall> get() = statements.flatMap { statement -> statement.calls }

val IrTypeOperatorCall.calls: List<IrCall> get() = argument.calls

val IrReturn.calls: List<IrCall> get() = value.calls

val IrVariable.calls: List<IrCall> get() = initializer?.calls.orEmpty()

/**
 * Recursively extracts all [IrCall]s from an [IrElement]
 */
val IrElement.calls: List<IrCall>
    get() = when (this) {
        is IrTypeOperatorCall -> calls
        is IrReturn -> calls
        is IrVariable -> calls
        is IrWhen -> calls
        is IrLoop -> calls
        is IrTry -> calls
        is IrContainerExpression -> calls
        is IrBody -> calls
        is IrCall -> listOf(this)
        else -> emptyList()
    }

val IrWhen.statements: List<IrStatementWithParent>
    get() = branches.flatMap { branch -> branch.result.allStatements() }

val IrLoop.statements: List<IrStatementWithParent>
    get() = body?.allStatements() ?: listOf(IrStatementWithParent(this))

val IrTry.statements: List<IrStatementWithParent>
    get() = tryResult.allStatements() +
            catches.flatMap { it.result.allStatements() } +
            finallyExpression?.allStatements().orEmpty()

/**
 * Recursively extracts all [IrStatement]s from an [IrElement]
 */
fun IrElement.allStatements(
    parent: IrElement? = null
): List<IrStatementWithParent> = when (this) {
    is IrContainerExpression -> statements.flatMap { it.allStatements(this) }
    is IrBody -> statements.flatMap { it.allStatements(this) }
    is IrWhen -> statements
    is IrLoop -> statements
    is IrTry -> statements
    is IrStatement -> listOf(IrStatementWithParent(this, parent))
    else -> emptyList()
}

data class IrStatementWithParent(val statement: IrStatement, val parent: IrElement? = null)

/**
 * Find the actual composable call for stubbing or verification, i.e.
 */
fun IrCall.extractComposableCallFromBlockArg(
    name: String,
): IrFunctionExpression {
    val composableBlockValueParameter = symbol.owner.valueParameters.find { valueParameter ->
        valueParameter.name.asString() == name
    } ?: pluginError("Failed to find @Composable function in lambda from $name.")

    val composableBlockValueArgument = getValueArgument(composableBlockValueParameter.index)
        ?: pluginError("No value argument found for ${composableBlockValueParameter.name}.")

    return composableBlockValueArgument.run {
        cast<IrFunctionExpression>() ?: extractComposableLambdaInstance()
    }
}

/**
 * The compose compiler may transform as follows:
 *
 * everyComposable(
 *     stubBlock = @Composable { ... }
 * )
 * ->
 * everyComposable(
 *     stubBlock = composableLambdaInstance(key = ..., tracked = ..., block = @Composable { ... })
 * )
 * or ->
 * everyComposable(
 *     stubBlock = ComposableSingletons$<Class with composable lambdas>.<get-lambda-n>()
 * )
 *
 * If that is the case, we need to extract the stub block from one of the composable lambda
 * containers instead of from the call directly.
 */
private fun IrExpression.extractComposableLambdaInstance(
    composableLambdaBlockParameterName: String = "block"
): IrFunctionExpression = when (this) {
    is IrCall -> when (symbol.owner.name.asString()) {
        // If this branch is matched we found a composableLambdaInstance(key, tracked, block = @Composable { ... })
        "composableLambdaInstance", "composableLambda" -> {
            val blockValueParameter = symbol.owner.valueParameters.find { valueParameter ->
                valueParameter.name.asString() == composableLambdaBlockParameterName
            } ?: pluginError(
                "Failed to find @Composable block in composableLambdaInstance from $composableLambdaBlockParameterName}. Parameter names were: ${symbol.owner.valueParameters.map { it.name }}."
            )

            val valueArgument = getValueArgument(blockValueParameter.index)
                ?: pluginError("No value argument found for ${blockValueParameter.name}.")
            valueArgument.cast<IrFunctionExpression>() ?: pluginError(
                "Assumed wrong type for value argument ${blockValueParameter.name}, expected ${IrFunctionExpression::class.java.name}, got ${valueArgument::class.java.name}."
            )
        }
        // If not, the call is most likely contained within ComposableSingletons$<class with lambdas>,
        // we'll backtrack from the return statement of the call.
        else -> symbol.owner.body
            ?.statements
            ?.lastOrNull()
            ?.cast<IrReturn>()
            ?.value
            ?.let { returnValue ->
                when (returnValue) {
                    is IrGetField -> returnValue.symbol.owner.initializer
                        ?.expression
                        ?.extractComposableLambdaInstance()
                    is IrCall -> returnValue.extractComposableLambdaInstance()
                    is IrFunctionExpression -> returnValue.extractComposableLambdaInstance()
                    else -> null
                }
            }
            ?: pluginError("Failed to find composableLambdaInstance from expression ${dumpKotlinLike()} with body ${symbol.owner.body?.dumpKotlinLike()}.")
    }
    is IrFunctionExpression -> this
    else -> pluginError("Failed to find composableLambdaInstance from expression ${dumpKotlinLike()}.")
}

/**
 * Recursively tries to find all @Composable calls from a function body.
 */
fun IrBody.findComposableCalls(): List<IrCall> = allStatements()
    .map(IrStatementWithParent::statement)
    .flatMap(IrStatement::calls)
    .filter(IrCall::isComposableFunctionCall)

/**
 * Transforms the $composer and $changed arguments that the Compose compiler generates into
 * argument matchers for the given mocking framework(s) that is in use.
 */
fun IrCall.transformComposeArgs(
    pluginContext: IrPluginContext,
    composerArgument: DeclarationIrBuilder.(IrExpression) -> IrExpression,
    changedArgument: DeclarationIrBuilder.(IrExpression) -> IrExpression,
    logger: Logger
) {
    val beforeTransform = dumpKotlinLike()

    val composerType = pluginContext.composerClassSymbol.defaultType.makeNullable()

    val composerArgIndex = (0 until valueArgumentsCount)
        .mapNotNull { index -> getValueArgument(index)?.let { index to it } }
        .find { (_, arg) -> arg.type == composerType }
        ?.first
    val composerArgMissing = symbol.owner.valueParameters.none {
        it.type.makeNullable() == composerType
    }

    if (composerArgMissing || composerArgIndex == null) {
        // Function has not been transformed with $composer and $changed if we get here.
        pluginError(
            "Composable function\n${symbol.owner.dumpKotlinLike()}was not transformed with ${"\$composer"} and ${"\$changed"} args. This most likely means that the Compose compiler plugin is not on the kotlinc classpath."
        )
    }

    putValueArgument(
        composerArgIndex,
        pluginContext.buildIr(symbol) {
            composerArgument(getValueArgument(composerArgIndex)!!)
        }
    )

    // $changed is always added as following $composer
    putValueArgument(
        composerArgIndex + 1,
        pluginContext.buildIr(symbol) {
            changedArgument(getValueArgument(composerArgIndex + 1)!!)
        }
    )

    val afterTransform = dumpKotlinLike()
    logger.log("Transformed $beforeTransform -> $afterTransform")
}

private const val COMPOSE_RUNTIME_PACKAGE_NAME = "androidx.compose.runtime"

class MockposablePluginException(message: String) : Exception(
    "$message\nThis is a bug in the Mockposable compiler plugin, please report the issue on GitHub."
)

fun pluginError(message: String): Nothing = throw MockposablePluginException(message)

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> Any?.cast(): T? = this as? T