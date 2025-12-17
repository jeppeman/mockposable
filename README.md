# Mockposable
A companion to mocking libraries that enables stubbing and verification of functions annotated with `@androidx.compose.runtime.Composable`.

It currently works on JVM and Android, and integrates with the following mocking libraries:
* [MockK](https://github.com/mockk/mockk)
* [Mockito](https://github.com/mockito/mockito)

## Why
It may come in handy if you need to stub certain functionality exposed by Compose UI, 
or if you're using Compose for purposes other than Compose UI, and want to do stubbing within that context.<br/>
Here are a couple of example use cases from the integration tests of the project:
* [Paparazzi](https://github.com/cashapp/paparazzi) snapshot tests with stubbed [collectIsPressedAsState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/interaction/package-summary#(androidx.compose.foundation.interaction.InteractionSource).collectIsPressedAsState()) in order to record and verify the look of pressed UI elements. <br/> See example [here](integration-tests/android/src/test/kotlin/com/jeppeman/mockposable/integrationtests/android/SnapshotTest.kt)
* Integration tests of a Composable function or Activity/Fragment in combination with a [Molecule](https://github.com/cashapp/molecule)-style presenter. <br/> See example [here](integration-tests/android/src/test/kotlin/com/jeppeman/mockposable/integrationtests/android/MoleculeStylePresenterTest.kt).

## How
The reason why we can't stub or verify `@Composable` functions in the first place is twofold: 

1) They are only allowed to run in a very particular context, namely in [androidx.compose.runtime.Composition.setContent](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composition#setContent(kotlin.Function0)).
2) The Compose compiler plugin transforms `@Composable` functions like so: <br/> `@Composable fun composableFun(args)` <br/> -> <br/> `@Composable fun composableFun(args, $composer: Composer, $changed: Int)` <br/> This means that even if we're able to provide a context to stub in, we can't provide matchers for the `$composer` and `$changed` arguments (unless we resort to reflection shenanigans), as a result, any stubbing we attempt will fail to be matched because the generated argument values will change.

This is addressed by:
1) Providing a way to run `@Composable` functions once for stubbing purposes, this then hooks into the stubbing mechanism of the mocking library that is being integrated with. 
2) A [Kotlin compiler plugin](mockposable/mockposable-compiler) that runs after the Compose compiler plugin has done its transformations. <br/> The plugin transforms calls to `@Composable` functions within a stubbing context like so: <br/> `composableFunctionCall(args = args, $composer = composer, $changed = changed)` <br/> -> <br/> `composableFunctionCall(args = args, $composer = any<Composer>(), $changed = any<Int>())` <br/> This allows us to stub and verify without caring about the values of `$composer` and `$changed`.

This approach is multiplatform friendly by virtue of doing compile time IR-transformations rather than runtime bytecode instrumentation.

## Usage
Apply and configure the Gradle plugin:
```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    
    dependencies {
         classpath 'com.jeppeman.mockposable:mockposable-gradle:0.16'
    }
}

apply plugin: 'com.jeppeman.mockposable'

// This is where you configure what libraries to integrate with, and what version of the compose 
// compiler to use.
mockposable {
    // You can add one or many, e.g:
    // plugins = ['mockk']
    // plugins = ['mockito']
    // plugins = ['compose-ui']
    // plugins = ['mockk', 'mockito', 'compose-ui']
    plugins = [...] // plugins = listOf(...) for build.gradle.kts

    // This is optional, and only relevant for Kotlin versions < 2.0, it defaults to the
    // version that mockposable uses internally.
    // If you as a consumer upgrade to a newer version of Kotlin before this plugin has had a 
    // chance to, you can explicitly select a version of the compose compiler plugin that is
    // compatible with the version of Kotlin you use.
    composeCompilerPluginVersion = "x.y.z"
}
```
This will apply the Kotlin compiler plugin as well as the relevant runtime dependencies.

### Stubbing and verification with MockK

The [MockK-companion](mockposable/mockposable-runtime/mockposable-runtime-mockk) provides counterparts for the MockK standard API that works with `@Composable` functions.


```groovy
mockposable {
    plugins = ['mockk']
}
```

```kotlin
import com.jeppeman.mockposable.mockk.everyComposable
import com.jeppeman.mockposable.mockk.verifyComposable

interface Dumb {
   @Composable 
   fun dumber(arg: Int): Int
}

@Test
fun `test something`() {
    val dumbMock = mockk<Dumb> {
        everyComposable { dumber(any()) } returns 42
    }

    ...
    
    verifyComposable { dumbMock.dumber(any()) }
}

// Stubbing top level composable functions

@Composable
fun topLevelComposable(): String {
    return "foo"
}

@Test
fun `test something else`() = mockkStatic("com.example.MyFilenameKt") { // The FQ name of the container class Kotlin creates for the top level function
    everyComposable { topLevelComposable() } returns "bar"
    
    ...
    
    verifyComposable { topLevelComposable() }
}

```
The full API surface of the MockK-companion comprises the following: 
| MockK Stub/verification function | Composable counterpart | 
| :------------- | :--------- | 
| <kbd>every</kbd> | <kbd>everyComposable</kbd> |
| <kbd>answers</kbd> | <kbd>answersComposable</kbd> |
| <kbd>andThenAnswer</kbd> | <kbd>andThenComposable</kbd> |
| <kbd>verify</kbd> | <kbd>verifyComposable</kbd> |
| <kbd>verifyAll</kbd> | <kbd>verifyComposableAll</kbd> |
| <kbd>verifyOrder</kbd> | <kbd>verifyComposableOrder</kbd> | 
| <kbd>verifySequence</kbd> | <kbd>verifyComposableSequence</kbd> |

### Stubbing and verification with Mockito
[Mockito-companion](mockposable/mockposable-runtime/mockposable-runtime-mockito). Same as MockK-companion but for Mockito.

```groovy
mockposable {
    plugins = ['mockito']
}
```

```kotlin
import com.jeppeman.mockposable.mockito.onComposable
import com.jeppeman.mockposable.mockito.verifyComposable

interface Dumb {
   @Composable 
   fun dumber(arg: Int): Int
}

@Test
fun `test something`() {
    val dumbMock = mock<Dumb> {
        onComposable { dumber(any()) } returns 9
    }

    ...
    
    verifyComposable(dumbMock) { dumber(any()) }
}

```
The full API surface of the Mockito-companion comprises the following: 
| Mockito Stub/verification function | Composable counterpart | 
| :------------- | :--------- | 
| <kbd>on</kbd> | <kbd>onComposable</kbd> | 
| <kbd>doAnswer</kbd> | <kbd>doAnswerComposable</kbd> |
| <kbd>verify</kbd> | <kbd>verifyComposable</kbd> |

### Stubbing and verification with Compose UI

In order to stub composables that gets emitted to the view tree in [Compose UI](https://developer.android.com/jetpack/compose) (such as [Text](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#Text(androidx.compose.ui.text.AnnotatedString,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.font.FontStyle,androidx.compose.ui.text.font.FontWeight,androidx.compose.ui.text.font.FontFamily,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextDecoration,androidx.compose.ui.text.style.TextAlign,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextOverflow,kotlin.Boolean,kotlin.Int,kotlin.Int,kotlin.collections.Map,kotlin.Function1,androidx.compose.ui.text.TextStyle))), there is a special [rule](https://github.com/jeppeman/mockposable/blob/main/mockposable/mockposable-runtime/mockposable-runtime-composeui/src/main/kotlin/com/jeppeman/mockposable/composeui/MockposableComposeRule.kt) that is needed in order to make sure that the stubbed composables are run with the right Composer, and hence emitted to the correct view tree: 

```groovy
mockposable {
    plugins = ['compose-ui']
}
```

```kotlin
@RunWith(AndroidJUnit4::class)
class MyTest {
    @get:Rule
    val composeTestRule = MockposableComposeRule(createComposeRule())

    @Test
    fun test() = mockkStatic("androidx.compose.material.TextKt") {
        everyComposable { Text(text = any()) } answersComposable { Text(text = "Will replace") }

        composeTestRule.setContent { Text(text = "Will be replaced") }

        composeTestRule.onNodeWithText("Will replace").assertIsDisplayed()
        composeTestRule.onNodeWithText("Will be replaced").assertDoesNotExist()
    }
}
```
