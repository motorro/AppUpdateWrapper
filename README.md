# AppUpdateWrapper [![Check](https://github.com/motorro/AppUpdateWrapper/actions/workflows/check.yml/badge.svg?branch=master)](https://github.com/motorro/AppUpdateWrapper/actions/workflows/check.yml) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.motorro.appupdatewrapper/appupdatewrapper/badge.png)](https://repo1.maven.org/maven2/com/motorro/appupdatewrapper/appupdatewrapper/)

A wrapper for [Android AppUpdateManager](https://developer.android.com/reference/com/google/android/play/core/appupdate/AppUpdateManager) 
to simplify in-app update flow. 
> Here is an [intoroduction article](https://proandroiddev.com/in-app-updates-for-android-made-easier-f57376ef7934) to this library describing problems I've come across when implementing in-app updates in a real-world Android application.


## Contents
<!-- toc -->

- [Features](#features)
- [Basics](#basics)
- [Using in your project](#using-in-your-project)
  * [1. Setting up the dependency](#1-setting-up-the-dependency)
  * [2. Implementing AppUpdateView](#2-implementing-appupdateview)
    + [activity (mandatory)](#activity-mandatory)
    + [updateReady (mandatory)](#updateready-mandatory)
    + [updateFailed (mandatory)](#updatefailed-mandatory)
    + [updateChecking (optional)](#updatechecking-optional)
    + [updateDownloadStarts (optional)](#updatedownloadstarts-optional)
    + [updateInstallUiVisible (optional)](#updateinstalluivisible-optional)
    + [updateComplete (optional)](#updatecomplete-optional)
    + [nonCriticalUpdateError (optional)](#noncriticalupdateerror-optional)
  * [3. Start your update flow](#3-start-your-update-flow)
    + [checkActivityResult](#checkactivityresult)
    + [userCanceledUpdate and userConfirmedUpdate](#usercanceledupdate-and-userconfirmedupdate)
    + [cleanup](#cleanup)
    + [Starting IMMEDIATE update](#starting-immediate-update)
    + [Starting FLEXIBLE update](#starting-flexible-update)
  * [Non-intrusive flexible updates with UpdateFlowBreaker](#non-intrusive-flexible-updates-with-updateflowbreaker)
- [Using library in multi-activity setup](#using-library-in-multi-activity-setup)
  * [AppUpdateManager instance](#appupdatemanager-instance)
  * [Use safe event handlers](#use-safe-event-handlers)
  * [A possible implementation](#a-possible-implementation)
- [Logging](#logging)
  * [Enabling logger](#enabling-logger)
  * [Logging rules](#logging-rules)

<!-- tocstop -->

## Features
-   A complete [lifecycle-aware component](https://developer.android.com/topic/libraries/architecture/lifecycle) to take
    a burden of managing update state (especially FLEXIBLE).
-   You should only implement a couple of UI methods to display install consents the way you like it to get running.
-   Makes things easier to run flexible update for multi-activity applications - suspends and resumes UI interaction
    along with main application flow.
-   Flexible updates are non-intrusive for app users with [UpdateFlowBreaker](#non-intrusive-flexible-updates-with-updateflowbreaker).
    
## Basics
Refer to [original documentation](https://developer.android.com/guide/playcore/in-app-updates) to understand 
the basics of in-app update. This library consists of two counterparts:
-   [AppUpdateWrapper](appupdatewrapper/src/main/java/com/motorro/appupdatewrapper/AppUpdateWrapper.kt) is a presenter 
    (or presentation model to some extent) that is responsible for carrying out the `IMMEDIATE` or `FLEXIBLE` update 
    flow. It is designed as a single-use lifecycle observer that starts the flow as soon as it is created. The wrapper 
    will also maintain a current state of update especially the UI state (the thing missing in original 
    `AppUpdateManager`). The update flow completes either with calling `updateComplete()` or `updateFailed(e: Throwable)`
    methods of `AppUpdateView`.
-   [AppUpdateView](appupdatewrapper/src/main/java/com/motorro/appupdatewrapper/AppUpdateView.kt) is a presenter 
    counterpart that you implement to ask user consents, notify of update errors, etc.

Here is the basic activity setup that is enough to run a flexible update in background each time your activity starts:
```kotlin
/**
 * Basic update activity that checks for update
 */
class TestActivity : AppCompatActivity(), AppUpdateView {
    /**
     * Update flow
     */
    private lateinit var updateWrapper: AppUpdateWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Starts flexible update flow that (if cancelled) will ask again tomorrow
        updateWrapper = startFlexibleUpdate(
            AppUpdateManagerFactory.create(this.applicationContext),
            this,
            UpdateFlowBreaker.forOneDay(getSharedPreferences("uiState", Context.MODE_PRIVATE))
        )
    }

    // Passes an activity result to wrapper to check for play-core interaction
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (updateWrapper.checkActivityResult(requestCode, resultCode)) {
            // Result handled and processed
            return
        }
        // Process your request codes
    }

    /********************************/
    /* AppUpdateView implementation */
    /********************************/

    // AppUpdateManager needs your activity to start dialogs
    override val resultContractRegistry: ActivityResultRegistry = this.activityResultRegistry

    // Called when flow starts
    override fun updateChecking() {
        // Show some spinner...
    }

    // Update is downloaded and ready to install
    override fun updateReady() {
        // Display confirmation dialog of your choice and complete update...
        updateWrapper.userConfirmedUpdate()
        // ...or cancel it
        updateWrapper.userCanceledUpdate()
    }

    // Update check complete
    override fun updateComplete() {
        Toast.makeText(this, "Update check complete", Toast.LENGTH_SHORT).show()
    }

    // Update check critical error (effective in IMMEDIATE flow)
    override fun updateFailed(e: Throwable) {
        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
        finish()
    }
}
```
    
## Using in your project
Follow the steps below to use `AppUpdateWrapper` in your application. Some of them are optional, some of them are 
required for multi-activity applications only. 

### 1. Setting up the dependency
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.motorro.appupdatewrapper:appupdatewrapper:x.x.x@aar'
}
```

### 2. Implementing AppUpdateView
`AppUpdateView` represents a UI part of application update flow and the only point of interaction between your 
application and `AppUpdateWrapper`. You may directly extend it in your hosting `Activity` or delegate it to some 
fragment. Here are the methods you may implement:

#### resultContractRegistry (mandatory)
```kotlin 
val resultContractRegistry: ActivityResultRegistry
```
 `AppUpdateManager` launches activities on behalf of your application. Implement this value to pass the activity 
result registry that will handle the `onActivityResult`. Typically you pass your activity  `activityResultRegistry`
there.

#### updateReady (mandatory)
```kotlin
fun updateReady()
```
Called in flexible flow to report that update has been downloaded and ready to be installed. The UI may display some confirmation 
dialogue at this point. Depending on user's answer call `AppUpdateWrapper.userConfirmedUpdate` to install and restart or
`AppUpdateWrapper.userCanceledUpdate` to postpone installation. The next time the user will be asked to update an app in
a latter case is [configurable](#non-intrusive-flexible-updates-with-updateflowbreaker).

#### updateFailed (mandatory)
```kotlin
fun updateFailed(e: Throwable)
``` 
Effective within the `IMMEDIATE` update flow to report a critical update error. Within the immediate update flow this is
considered critical and you may want to terminate application.

#### updateChecking (optional)
```kotlin
fun updateChecking()
```
Called by presenter when update flow starts. UI may display a spinner of some kind.

#### updateDownloadStarts (optional)
```kotlin
fun updateDownloadStarts()
```
Called when user confirms flexible update and background download begins. 
Called in flexible flow.

#### updateInstallUiVisible (optional)
```kotlin
fun updateInstallUiVisible()
```
Called when play-core install activity covers your application. In some cases (immediate update flow for example) you
may want to finish your main activity (or clear stack) at this point so user won't 'back' to it from update screen.

#### updateComplete (optional)
```kotlin
fun updateComplete()
```
Called when flow is finished. Either update not found, or cancelled, etc. Indicates a successful outcome.
Called in flexible flow. Immediate flow either restarts (on success) or fails with [updateFailed](#updatefailed-mandatory).

#### nonCriticalUpdateError (optional)
```kotlin
fun nonCriticalUpdateError(e: Throwable)
```
Some error occurred during the update but it is considered non-critical within selected update flow. UI may toast or 
notify the user in some other way.
Called in flexible flow. Immediate flow is designed to be mandatory so it fails with critical [updateFailed](#updatefailed-mandatory).

### 3. Start your update flow
The library supports both `IMMEDIATE` and `FLEXIBLE` update flows. 
-   Choose `IMMEDIATE` flow when you need user to update or to quit. For example you may receive a 'not-supported' 
    message from your server and user can't use the outdated application anymore.
-   Choose `FLEXIBLE` flow when you just check if there is a new version available. User may install it or may reject to
    do so. In the latter case it is considered a good UX practice to not to annoy user with every activity start but 
    rather to delay and ask him to update later. See [UpdateFlowBreaker](#non-intrusive-flexible-updates-with-updateflowbreaker) below for
    details.

Both flows implement the `AppUpdateWrapper` interface with the following methods to consider:

#### userCanceledUpdate and userConfirmedUpdate
```kotlin
fun userCanceledUpdate()
fun userConfirmedUpdate()
```
When flexible flow download completes you should ask for user consent with install and application restart.
At this point `AppUpdateWrapper` calls your view [updateReady](#updateready-mandatory) callback. Present a consent UI of
your choice and call either `userConfirmedUpdate` or `userCanceledUpdate`. See the [UpdateFlowBreaker](#non-intrusive-flexible-updates-with-updateflowbreaker)
below to find out what happens if user cancels.

#### cleanup
```kotlin
fun cleanup()
```
Call this method to abort update check and cleanup all update flow internals. Sometimes you may need to cancel the 
flexible update check to start immediate flow right away. So you may end up with the following in your host activity:
```kotlin
/**
 * Update wrapper instance
 */
private var updateWrapper: AppUpdateWrapper? = null
    set(value) {
        field?.cleanup()
        field = value
    }
    
/**
 * Starts immediate application update
 */
fun updateRightAway() {
    updateWrapper = startImmediateUpdate(appUpdateManager, this)
}

/**
 * Checks if update available
 */
fun checkForUpdate() {
    updateWrapper = startFlexibleUpdate(
        appUpdateManager,
        this,
        flowBreaker
    )
}
 
``` 

#### Starting IMMEDIATE update
Choose `IMMEDIATE` flow when you need user to update or to quit. For example you may receive a 'not-supported' 
message from your server and user can't use the outdated application anymore. To start immediate flow call the following
function from your lifecycle owner (activity):
```kotlin
fun LifecycleOwner.startImmediateUpdate(appUpdateManager: AppUpdateManager, view: AppUpdateView): AppUpdateWrapper
``` 
Parameters: 
-   `appUpdateManager` - application update manager instance
-   `view` - your `AppUpdateView` implementation

This flow requires no user interaction on your side besides that you should implement [updateFailed](#updatefailed-mandatory)
as you'll want to shutdown your application in that case.
The flow ends up in application being restarted (so you may not see the `updateComplete` call) or failure (above).

#### Starting FLEXIBLE update
Chose `FLEXIBLE` update if you just want to notify user if update is available but there is no problem if he cancels it.
To start the flow call the following function from your lifecycle owner (activity):
```kotlin
fun LifecycleOwner.startFlexibleUpdate(
    appUpdateManager: AppUpdateManager,
    view: AppUpdateView,
    flowBreaker: UpdateFlowBreaker
): AppUpdateWrapper
```
Parameters:
-   `appUpdateManager` - application update manager instance
-   `view` - your `AppUpdateView` implementation
-   `flowBreaker` - the delegate that will acknowledge user cancellation and make sure you won't bother him too much.
    More on this follows. 
    
If update is available, the flow displays a consent with download window (play-core internal activity). So make sure to 
pass the `onActivityResult` data to [checkActivityResult](#checkactivityresult). When update is downloaded, the wrapper 
calls your [updateReady](#updateready-mandatory) callback where you should call either `userConfirmedUpdate` or 
`userCanceledUpdate` of workflow to continue.  

### Non-intrusive flexible updates with UpdateFlowBreaker
One of the possible ways to implement a non-intrusive flexible workflow is to run it in background when your activity 
starts and notify user the update is available. The user continues to use application normally while update check runs.
If user cancels the update he will be prompted again when an activity starts again which is a bit annoying. To overcome
this flaw the flexible update of this library incorporates a couple of simple interface that are used to postpone update 
checks if user has already declined:
```kotlin
/**
 * Checks if user has already refused to install update and terminates update flow
 */
interface UpdateFlowBreaker: TimeCancelledStorage {
    /**
     * Checks if enough time has passed since user had explicitly cancelled update
     */
    fun isEnoughTimePassedSinceLatestCancel(): Boolean

    /**
     * An extra point to check if update available is of any value to user
     * For example you may implement extra logic on update priority level (set from play market) or
     * staleness. Called _instead_ of [isEnoughTimePassedSinceLatestCancel] when processing
     * available update so you may want to combine both time of last offer and update value at the
     * same time.
     * @param updateInfo
     */
    fun isUpdateValuable(updateInfo: AppUpdateInfo): Boolean
}

/**
 * Stores time the update was cancelled
 */
interface TimeCancelledStorage {
    /**
     * Gets the latest time user has explicitly cancelled update (in milliseconds)
     */
    fun getTimeCanceled(): Long

    /**
     * Saves current time as the latest one user has explicitly cancelled update
     */
    fun saveTimeCanceled()
}
```
You may want to implement them yourself but the library has some already. They are created by calling corresponding
factory functions of [UpdateFlowBreaker](appupdatewrapper/src/main/java/com/motorro/appupdatewrapper/UpdateFlowBreaker.kt):
```kotlin
fun alwaysOn(): UpdateFlowBreaker
```
Creates a breaker that is always turned on and never breaks the flow.
```kotlin
fun withInterval(interval: Long, timeUnit: TimeUnit, storage: TimeCancelledStorage): UpdateFlowBreaker
```
Used to postpone check for `interval` since user explicitly cancelled
```kotlin
fun forOneDay(storage: TimeCancelledStorage): UpdateFlowBreaker
```
Postpones update for one day
```kotlin
fun forOneDay(storage: SharedPreferences): UpdateFlowBreaker
```
Postpones update for one day storing cancellation time in shared preferences. 
```kotlin
fun UpdateFlowBreaker.withUpdateValueCheck(valueCheck: (Boolean, AppUpdateInfo) -> Boolean): UpdateFlowBreaker
```
Extends the existing breaker with an update value check function [valueCheck] which is called
when update info gets available. Update priority or staleness combined with last update offer time 
will hel you to decide if to ask again or even cancel the flexible flow and switch to immediate flow.
The function you pass will receive a time check result from your wrapped breaker and an update info
from update manager.
   
Take a look how to pass the breaker to the workflow in a [basic example](#basics).

## Using library in multi-activity setup
When the app is based on multiple activities it presents an extra challenge to implement a flexible update  due to 
multiple `onResume`, `onPause` workflow events. The update may start in one activity and the other one should respond to
download complete event and present an install consent. Most (hopefully) of these cases are addressed in library design
and tested in a large multi-activity application. It seems however the original `AppUpdateManager` was written with 
single-activity design in mind and several issues are to be solved.

### Start update everywhere
The library does not store any common update state between wrapper instances. The only state it uses
is what google play-core provides. This is done intentionally as the update is downloaded without your
control and there is no way to stay sync with it unless you are in a foreground (so-so). Thus in order 
to catch-up with things going on, the wrapper should be started **in any activity that may take part in 
update process** (interact with user concerning update). The update flow will be updated to correct
state as long as the google-provided status allows to do it.

### AppUpdateManager instance
Consider using a singleton instance injecting it to your flow instances from an application scope. Using several 
`AppUpdateManager` instances simultaneously didn't work for me.

### Use safe event handlers

> Google has fixed the issue with event listeners in 'com.google.android.play:core:1.6.3'
> If you use library 1.3.3 and above you don't need this fix

When using a singleton `AppUpdateMaanager` with multiple activities there is a problem with managing subscription to 
update flow events. When several listeners are connected to a single manager and try to usnsubscribe in an event handler
the manager crashes with `ConcurrentModificationException`. This is due to a non-copy hash-set iteration is used to 
notify listeners internally. See the [test file](appupdatewrapper/src/test/java/com/motorro/appupdatewrapper/SafeListenersKtTest.kt)
for the details. To overcome the problem for a moment the library uses a duct-tape solution which will hopefully be 
removed in the future. All-in-all if you are using a multi-activity setup and come across such exceptions (see when
download completes) turn on the solution prior to your flow is created like this:
```kotlin
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        AppUpdateWrapper.USE_SAFE_LISTENERS = true
    }
}
```

### A possible implementation
You may want to implemented the `AppUpdateView` in your base activity (if you have any) injecting it 
with a singleton `AppUpdateManager`.
I will not put a complete listing here but you may take a look at the following gists for complete reference:
* [Base activity setup](https://gist.github.com/motorro/ceeca40e9947ab1c142fe037c9613dbd)
* [Dagger providers for update wrapper](https://gist.github.com/motorro/51587a5c652bad49a6220da17d7572fc)

## Logging
Sometimes you'll want to see what is going on in the update flow. The library supports logging to 
[Timber](https://github.com/JakeWharton/timber). 

### Enabling logger
The library itself does not plant any tree - you need to do it yourself to get log output:
```kotlin
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

### Logging rules
*   All library log output has common tag prefix: `AUW`. 
*   Checking for update and update results have `info` level.
*   Update check failure and critical errors have `warning` level.
*   Internals (state transition, lifecycle updates) have `debug` level.

All-in-all the library log looks like this:
```
D/AUW::AppUpdateLifecycleStateMachine: State machine initialized
D/AUW:startImmediateUpdate: Starting FLEXIBLE update flow...
D/AUW::AppUpdateLifecycleStateMachine: Setting new state: Initial
D/AUW::Initial: onStart
D/AUW::AppUpdateLifecycleStateMachine: Setting new state: Checking
D/AUW::AppUpdateLifecycleStateMachine: Starting new state...
D/AUW::Checking: onStart
D/AUW::IntervalBreaker: Last time cancelled: 0, Current time: 1565980326128, Enough time passed: yes
I/AUW::Checking: Getting application update info for FLEXIBLE update...
I/AUW::Checking: Application update info: Update info: 
        - available version code: 62107400
        - update availability: UPDATE_NOT_AVAILABLE
        - install status: UNKNOWN
        - update types allowed: NONE
D/AUW::Checking: Evaluating update info...
```
