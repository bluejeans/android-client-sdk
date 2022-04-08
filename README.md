[![BlueJeans Android Software Development Kit](https://user-images.githubusercontent.com/23289872/127987669-3842046b-2f08-46e4-9949-6bf0cdb45d95.png "BlueJeans Android Software Development Kit")](https://www.bluejeans.com "BlueJeans Android Software Development Kit")

# BlueJeans Android Client Software Development Kit

The BlueJeans Android Client Software Development Kit (SDK) gives a quick and easy way to bring an immersive video-calling experience into your android applications.

With BlueJeans Android Client SDK, participants can join video conference meetings where they receive individual video streams from each of the video participants in the meeting. This provides an enhanced remote video quality experience with the resolution, fps of individual streams better as compared to a single composited stream in an earlier hybrid model.

## Features :
- Audio and Video Permission handling
- Join, End Meeting
- Self Video
- Remote Audio, Remote Video states
- Content receive 
- Audio and Video self mute
- Orientation handling
- Video device enumeration, Selection
- Audio device enumeration, Selection
- Video Layout switch
- Participant list
- Participant properties: Audio mute state, Video mute state, is Self, Name and Unique Identifier
- Self Participant
- Screen Share
- Log Upload
- Multi-stream support (Sequin Video Layouts)
- Enable torch/flash unit on a device
- Set capture requests such as zoom, exposure on the active video device
- Public and Private meeting Chat
- Incoming Audio, Video and Content mute
- Meeting Information (Title, Hostname, URL) property
- 720p video capture (Experimental API)
- Moderator Controls 
  - Meeting recording
  - Mute/UnMute Audio/Video of other participants / all participants
  - Remove a participant from the meeting
  - End meeting for all immediately or after a certain delay
- Audio capture dumps (debug facility)
- Waiting room
- Active Speaker
- Remote and Local mute information

## Current Version : 1.3.2

## Pre-requisites :
- **Android API level :** Min level 26

- **Android Device :**
   - OS level - Oreo 8.0 or later
   - CPU - armeabi-v7a, arm64-v8a
   - No support for emulator yet

- **Android Project & Gradle Settings:**
   - Android X
   - Compile SDK Version: 28 and above
   - Source and Target compatibility to java version 1_8 in gradle
   - RxJava, RxKotlin
   

## API Architecture
![Android SDK API Structure](https://user-images.githubusercontent.com/92993169/139242965-fb748808-e191-4308-be4d-4dc682f53c56.png)


## SDK Documentation :
Detailed documentation of SDK functions is available [here](https://bluejeans.github.io/android-client-sdk)

## How it all works?
You can experience BlueJeans meetings using the android client SDK by following the below 2 steps -

### Generate a meeting ID :
As a prerequisite to using the BlueJeans Android Client SDK to join meetings, you need to have a BlueJeans meeting ID. If you do not have a meeting ID then you can create one using a meeting schedule option using a BlueJeans account as below
   - Sign up for a BlueJeans Account either by opting in for a [trial](https://www.bluejeans.com/free-video-conferencing-trial) or a [paid mode](https://store.bluejeans.com/)
   - Once the account is created, you can schedule a meeting either by using the account or through the [direct API](https://bluejeans.github.io/api-rest-howto/schedule.html) calls. In order to enable API calls on your account, please reach out to [support team](https://support.bluejeans.com/s/contactsupport).

### Integrate BlueJeans Android Client SDK
Integrate the SDK using the below guidelines and use SDK APIs to join a meeting using the generated meeting ID. 

## Integration Steps :
### Override Minimum SDK Version :
This version of BlueJeans Android Client SDK is compatible with Android version "26" or higher. Therefore, your Android app must have a minimum SDK version 26 or higher.

If your app runs with min SDK version below API level 26, you must override min SDK version as in the below sample. However please note that, SDK instantiation will fail if the app runs on API level below 26, please add build check to avoid SDK instantiation on device with API level < 26.

**Sample Code:** Add the below to your AndroidManifest.xml
```xml
<uses-sdk android:minSdkVersion="26"
tools:overrideLibrary="com.bluejeans.bluejeanssdk"/>
```

### Install BlueJeans Android Client SDK:

We distribute our SDK from the Maven Repository.

To add the SDK to your app, add the following dependency in your build.gradle files:

In top-level project build.gradle
```xml
repositories { maven { url "https://swdl.bluejeans.com/bjnvideosdk/android" } }
```

In app's build.gradle
```xml
implementation "com.bluejeans:android-client-sdk:1.3.2"
```

### Upgrade Instructions :
Whenever a newer version of SDK is available, you can consume it by increasing the version in the implementation code block to the new SDK version.


## Initialize BlueJeans SDK :
Create the object of BlueJeans SDK in application onCreate with help of application context and use it to access all the APIs

The minimum permission needed to join a meeting is permission for RECORD_AUDIO. Make sure app requests this permission before calling Join API.

Sample Code :
```java
blueJeansSDK = new BlueJeansSDK(new BlueJeansSDKInitParams(this));
```

APIs are grouped into relevant services as in the architecture diagram. All the service objects are available all the time after SDK instantiation, however all are not active all the time.
When inactive, APIs of the services do not react and the subscriptions will yield null. 

**List of services :** 

**_Globally active services_** -> MeetingService, VideoDeviceService, LoggingService and PermissionService.

**_InMeeting active services_** -> ContentShareService, AudioDeviceService, ModeratorControlsService, PublicChatService, PrivateChatService, ParticipantsService, ModeratorWaitingRoomService and ClosedCaptioningService

InMeeting services are operational when _MeetingState_ transitions from _MeetingState.Connecting_ to _MeetingState.Connected_ and get inactivated
when the meeting ends by the transition of meeting state to _MeetingState.Idle_.

PermissionService : Provides for permission handling related APIs (refer to documentation for API set and details)

```java
blueJeansSDK.getPermissionService
```
LoggingService : Provides for SDK logging related APIs (refer to the documentation for API set and details)

```java
blueJeansSDK.getLoggingService
```

VideoDeviceService : Provides for video device enumeration and self video preview enablement APIs (refer to the documentation for API set and details)

```java
blueJeansSDK.getVideoDeviceService
```

MeetingService : Provides for meeting-related APIs and all inMeeting Services

```java
blueJeansSDK.getMeetingService
```

## Join a BlueJeans meeting :
It is recommended to start a foreground service before getting into the meeting. If you are not familiar with [foreground services](https://developer.android.com/guide/components/foreground-services) and [notfications](https://developer.android.com/reference/android/app/Notification), we suggest you to learn about these before proceeding with this section.

Starting a foreground service ensures we have all the system resources available to our app even when in the background, thereby not compromising on audio quality, content capture quality during features like content share and also prevents app from being killed due to lack of resources in the background.

Refer *OnGoingMeetingService* and *MeetingNotificationUtility* for sample implementation.

**Note:**
- foreground service is not needed if your app runs on a platform where it will never be put to background.

### Steps to join the meeting
- Provide Mic(RecordAudio) and Camera Permissions either by using BJN SDK permissionService or by Android SDK APIs
- Get and add *SelfVideoFragment* and *enableSelfVideoPreview* to start the self video
- Get and use meeting service and invoke join APIs to join a meeting
- Observe the meeting state by subscribing to the `meetingState` observable offered by `MeetingService`.

#### Following are the states which can be observed by subscribing to `meetingState` :
1. `Idle` is a state which determines that a meeting is not in progress.
2. `Validating` determines that the join request is being validated by the back end.
3. `WaitingRoom` is a state which is received when waiting room you try to join a waiting room enabled meeting as a participant. Please click [here](#waiting-room-) to learn more about `Waiting Room`.
4. `Connecting` is a state which determines that meeting was successfully authenticated and you are about to join the meeting.
5. `Reconnecting` is same as `Connecting`. This state is produced when a network glitch occurs.
6. `Connected` this state determines that you are finally into the meeting.

Below is a diagram depicting the meeting state transitions:
<img width="941" alt="Meeting-States" src="https://user-images.githubusercontent.com/23289872/139293856-8547c442-8eac-4321-b71c-6d7644d7ad06.png">

### Waiting Room :
Waiting room feature enables the controlled access to a meeting and allows the moderator to choose who can join/access its meeting, either directly or selectively. When enabled, participants would be first placed into a waiting room and moderator can then selectively allow or reject the respective join request from the waiting room participants list. Participants who get placed in waiting room have no way to prove their credentials/identity and the only information that moderator would have about them, is their name.

Waiting room feature for a meeting is enabled for all by default. In case you find it disabled for your meeting, please contact your admin or reach out to BlueJeans support.

![Flow chart](https://user-images.githubusercontent.com/23289872/139293102-23160b34-38eb-476b-9284-c4332dd16842.png)

#### VideoDeviceService (Video device enumeration, Selection) :

*enableSelfVideoPreview* provides for enabling/disabling the camera capturer. Functional irrespective of meeting state.

*videoDevices* will provide a list of video/camera devices available on the hardware.

*currentVideoDevice* will provide for the current device selected. By default, the front camera is selected.

Use *selectVideoDevice* and choose the video device of your choice from the available *videoDevices* list.

##### Setting capture request :

BlueJeans SDK provides for capability to *setRepeatingCaptureRequest* which internally deligates the request to Camera2's [setRepeatingRequest](https://developer.android.com/reference/android/hardware/camera2/CameraCaptureSession#setRepeatingRequest(android.hardware.camera2.CaptureRequest,%20android.hardware.camera2.CameraCaptureSession.CaptureCallback,%20android.os.Handler)) on the currently running camera session. This opens up options to set any [CaptureRequest](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest) of integrator's choice. Most commonly used option could be to set Zoom, the same is show cased in the sample test app.

##### Setting torch mode :

BlueJeans SDK provides for the capability to turn ON/OFF the torch. *setTorchMode* API sets the flash unit's torch mode of the video device for the given ID. Note : Some of the devices need the torch supported camera device to be open for the torch to be turned ON.

##### Enabling 720p Video Capture :

BlueJeans SDK provides for an experimental feature of 720p video capture. 

###### Pre requisites to enable 720p.
- number of CPU cores on the device must be 8 or above.
- the current video device state should be `VideoDeviceState.Opened`.
- the current video device should be capable of doing 720p. Check for capability at `VideoDevice.is720pCapable`.
- good bandwidth of close to 1500kbps on sender and receiver.

**This is an experimental API, still under performance evaluations, optimizations and hence not qualified for production use. Note that enabling 720p is CPU intensive and can cause device heat up, battery drain. It is recommended to build a graceful switch on this feature at the app layer
to provide better app performance experience**

###### Additional information :
- If the exact 1280x720 resolution is not available with the currently open video device, we pick the closest resolution to 720p starting from 1024x768 till 1440x1080.
- Switching to different video devices will turn off the 720p enablement.
- If you wish to turn 720p back and if the new video device selected is capable, then call this API again once the video device state is `VideoDeviceState.Opened`.
- Use `is720pEnabled` observable to track the enablement and disablement of 720p.
- The reciever app should support 720p receive in order to receive 720p from the SDK endpoint. Note that the Android Client SDK currently does not support 720p receive and the BlueJeans desktop, Desktop browser solutions support 720p receive.

###### Feature availability :
Some features (such as closed captioning) are not available in every meeting. Their availability depends on the account of the meeting owner, and the enterprise they belong to. The SDK provides methods to check if these features are available (e.g. isWaitingRoomCapable). To enable a feature that is not available on your account, please contact your enterprise admin and get the feature enabled through [BlueJeans support team](https://support.bluejeans.com/s/contactsupport).

## Meeting Service :
This service takes care of all meeting-related APIs. Apart from meeting related APIs, the service also provides provides for several inMeeting services - ParticipantsService, AudioDeviceService, ContentShareService, PublicChatService, PrivateChatService, ClosedCaptioningService and ModeratorWaitingRoomService.

### Video Layouts :

Represents how remote participants videos are composed
- **Speaker** : Only the most recent speaker is shown, taking up the whole video stream.
- **People** : The most recent speaker is shown as a larger video. A filmstrip of the next (up to) 5 most recent speakers is shown at the top.
- **Gallery** : A bunch of the most recent speakers is shown, arranged in a grid layout of equal sizes.

*videoLayout* provides for the current video layout *setVideoLayout* can be used to force a Video Layout of your choice.

Note that by default the current layout will be the People layout or it will be the one chosen by the meeting scheduler in his accounts meeting settings.


#### Different layouts, number of tiles :
- `Speaker layout` to fit one single active speaker participant
- `People layout` to fit max 6 participants, 1 (main active speaker participant) + 5 (film strip participants)
- `Gallery layout` can fit maximum number of participant tiles as 9 or 25 depending on SDK input configuration. By default it is 9 participants, ordered in 3x3 style. This is configurable to support max of 25 participants, ordered in 5x5 style

#### Configuring 5x5 in gallery layout:
BlueJeansSDKInitParams provides a new input parameter called videoConfiguration which can be set with value GalleryLayoutConfiguration.FiveByFive. It is recommended to set this only on larger form factor (>= 7") devices for a better visual experience. Note that using 5x5 will consume higher memory, CPU and battery as compared to other layouts

### Remote Video :

The BlueJeans SDK's RemoteVideoFragment provides for both the audio and video participant tiles. The organization and the ordering of these tiles depend on factors namely recent dominant speaker and meeting layout, in addition to an algorithm that ensures minimal movement of tiles when the recent speaker changes. Video participants are given the priority and are put first in the list and then the audio participants follow.

Note: MultiStream mode is not supported on devices with a number of CPU cores less than six. In such cases, RemoteVideoFragment would receive a single composited stream (participant's videos are stitched at the server, organized based on the layout chosen and a single stream is served to the client).

#### Participant background colour : 

By default, the participant tile when video is turned off shows a colour gradient with colour as blue at 90 degrees. This gradient colour and the gradient angle can be changed by the consumer app.

Below are the steps to achieve the same:

- Create a file with the name bjn_background_participant_tile.xml 
- Add a shape with the colour and gradient angle as per your choice into the file. Find the sample for the shape as below
```java
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="90"
        android:endColor="#073571"
        android:startColor="#6589a4" />
</shape>
```
- Save and copy the file to the application resource drawable folder 
- Build, Run the project
  
### Video Resolutions and BW consumption:

- Video receive resolution and BW max:

| Layout       | Max participants| Layout pattern for max participants| Video Receive max resolution, FPS                         | Video Receive BW max |
| -------------|:---------------:| :---------------------------------:| :--------------------------------------------------------:|:--------------------:|
| Speaker View | 1               | 1                                  | 640x360/640x480  30fps                                    | 600 kbps             |
| People View  | 6               | 1 (main stage) + 5 (film strip)    | main stage 640x360/640x480 30fps (if single participant)  | 600 kbps             |
|              |                 |                                    | main stage 320x180/240x180 30fps (if > 1 participant)     | 300 kbps             |
|              |                 |                                    | film strip 160x90/120x90, 15fps                           | 400 kbps             |
| Gallery View | 9               | 3x3 (landscape) / 4x2+1 (portrait) | 640x360/640x480 (participants < 2)      30 fps            | 1200 kbps            |
|              |                 |                                    | 320x180/240x180 (participants > 2, < 4) 30 fps            | 1200 kbps            |
|              |                 |                                    | 160x90/120x90  (participants > 4)       15 fps            | 900 kbps             |
|              |                 |                                    | 160x90/120x90  (participants > 9)       15 fps            | 1700 kbps            |

- Content receive resolution and BW max: 1920x1080 at 5 fps, 300 kbps
- Video send resolution and BW max: 640x480 at 30fps, 900 kbps

Note: Endpoints that send video in an aspect ratio of 4:3 instead of 16:9, will result in video receive a resolution of 640x480 in place of 640x360, 240x180 in place of 320x180, and 120x90 in place of 160x90. Mobile endpoints / BlueJeans android SDK endpoints send video at 640x480 i.e at an aspect ratio of 4:3.

### Mute:

The BluejeansSDK provides APIs to mute/unmute self video.

`enableSelfVideoPreview` of VideoDeviceService controls video device enablement. This drives the self video preview.
`setVideoMuted` of MeetingService will mute/unmute the self video stream flowing to the other endpoint. This API additionally triggers self video preview states internally.
 Available when meeting state moves to MeetingState.Connected

Note that
- video mute state applied on `enableSelfVideoPreview` needs to be applied to `setVideoMuted` once meeting state changes to MeetingState.Connected.
- when in a meeting (meeting state is MeetingState.Connected) and if `setVideoMuted` is called with true, `enableSelfVideoPreview` is called with true,
then the self video preview gets activated but the stream does not flow to the other endpoint.

#### Mute/Unmute Incoming Video :
The BluejeansSDK MeetingService provides API to mute, unmute remote participants video. This is helpful in the scenarios where the user does not intend to view remote video.
Some example use cases can be
- App has a view pager with the first page showing remote video and the second page showing content. When a user is on the content page, this API can be used to mute remote video.
- To provide an audio only mode
- App going in the background
**Note:** This API does not give instant result, this may take up to 5 sec in case of back-to-back requests.

##### API:
`meetingService.setRemoteVideoMuted(muted: Boolean)`

#### Mute/Unmute Incoming Audio :
The BluejeansSDK MeetingService provides API to mute, unmute remote audio. This is helpful in the scenarios where the user does not intend to hear incoming audio from remote participants.
Some example use cases can be
- Pause BlueJeans meeting to facilitate a concurrent VOIP call.
- Content Only mode where Audio and Video are disabled

##### API:
`meetingService.setRemoteAudioMuted(muted: Boolean)`

#### Mute/Unmute Content :
The BluejeansSDK MeetingService provides API to mute, unmute content. This is helpful in the scenarios where the user does not intend to view content.
Some example use cases can be
- App has a view pager with the first page showing remote video and the second page showing content. When user a is on the video page, this API can be used to mute content.
- To provide an audio only mode
- App receiving content and goes in to the background
**Note:** This API does not give instant results, this may take up to 5sec in case of back-to-back requests. Unlike for video, we have a single API to mute content share and mute content receive. Ensure to call this only when you are not sharing the content from your end.

##### API :
`meetingService.setContentMuted(muted: Boolean)`

#### Background handling recommendations :
When the app is put to background and the user is out of the meeting: User's self video needs to be stopped to save CPU load, save battery
When the app is put to background and the user is in a meeting:
- User's self video needs to be stopped for privacy reasons
- Remote video and content receive should be muted to save bandwidth

and the same can be turned ON when the app is put back to the foreground.

These can be achieved using the set of mute APIs SDK provides.
Use `setVideoMuted` for managing self video flowing to other endpoints when in a meeting
Use `enableSelfVideoPreview` for managing the capturer when not in the meeting
Use `setRemoteVideoMuted` for managing remote participants videos when in a meeting
Use `setContentMuted` for managing content when in a meeting

## Audio Device Service (Audio device enumeration, Selection) :

```java
blueJeansSDK.getMeetingService().getAudioDeviceService()
```

*audioDevices* will provide a list of audio devices available on the hardware.

*currentAudioDevice* will provide for the current audio device selected.

On dynamic change in audio devices, SDK's default order of auto selection is as below:

- BluetoothHeadset
- USBDevice
- USBHeadset
- WiredHeadsetDevice
- WiredHeadPhones
- Speaker

Use *selectAudioDevice* and choose the audio device of your choice from the available *audioDevices* list.

## Participants Service :

```java
blueJeansSDK.getMeetingService().getParticipantsService()
```

*participant* represents a meeting participant. Carries properties to identify whether video and audio is muted or not, MuteState to differentiate if is locally or remotely muted for both audio and video, is self, name and an unique identifier of the participant.

*participants* provides for a list of meeting participants. The list will be published on any change in the number of meeting participants or the change in properties of any of the current participants. Any change will reflect in the content of the list and the list reference remains the same throughout the meeting instance.

*selfParticipant* represents self. Provides for any changes in properties of the self.

*activeSpeaker* represents the current actively speaking participant. Some of the use cases are:
- Display active speaker whenever video is hidden, mainly in content only / low bandwidth mode
- Keep track of participants who have spoken in a meeting

### Content Share Feature:

Provides facility to share content within the meeting, thereby enhances the collaborative experience of the meeting session. Our SDK currently supports content share in the form of full device's screen share which can be accessed via ContentShareService
```java
blueJeansSDK.getMeetingService().getContentShareService()
```

#### Sharing the Screen :
This is a full device screen share, where the SDK will have access to all of the information that is visible on the screen or played from your device while recording or casting. This includes information such as passwords, payment details, photos, messages, and audio that you play. This information about data captured is prompted to the user as a part of starting screen share permission consent dialog and the data populated within the dialog comes from android framework.

##### Feature pre requisites :
- Permission to capture screen data using android's Media Projection Manager
- Foreground service

##### Sample code to ask permission :

      MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
      startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_SHARE_REQUEST_CODE);

The above request will result in a permission dialog like

<img width="320" alt="ScreenSharePermissionDialog" src="https://user-images.githubusercontent.com/23289872/114588998-e2e28e00-9ca4-11eb-850c-7b0396a068fd.png">

Note that, this is a system prompted dialog where the name in the dialog will be the app name and the content is predefined by the Android System.

##### Start foreground service, Screen Share :
Once you get the result of the permission, start a service and then invoke _startContentShare_ API :

**Note:** If you already have a foreground service running for the meeting, then an explicit service for screen sharing is not needed.
Apps targeting SDK version `Build.VERSION_CODES.Q` or later should specify the foreground service type using the attribute `R.attr.foregroundServiceType`
in the service element of the app's manifest file as below

      <service
         android:name=".YourForegroundService"
         android:stopWithTask="true"
         android:foregroundServiceType="mediaProjection"/>

##### Invoke content share start API :

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SCREEN_SHARE_REQUEST_CODE:
                if (data != null) {
                     startYourForegroundService() // start your service if there is not one already for meeting
                     contentShareService.startContentShare(new ContentShareType.Screen(data));
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

### Start Share API :
`startContentShare(contentShareType: ContentShareType)`

### Stop Share API :
`stopContentShare()`

#### Observables with Screen Share feature :
- contentShareState - provides for screen share current state
- contentShareEvent - provides for screen share events
- contentShareAvailability - provides for information about content share feature being available or not. Content share may not be available in cases such as an access disabled by a moderator, access disabled by admin, due to lack of meeting privilege, and enablement of a moderator only share.

#### User experience recommendation :
Whenever Screen Share starts,
- Put the app to the background thereby easing the user to chose the screen/app of his choice
- Have an overlay floater button out of the app, which can be used to stopping the screen share
- Stopping screen share using the floater button can bring the app back to the foreground
- Put an overlay border around the screen to indicate that the screen share is in progress

## Meeting Chat Feature :
The Bluejeans SDK's MeetingService provides a facility to chat within the meeting with participants present in the meeting. The chat will remain during the
duration of the meeting and will be cleared once the meeting is over.

There are two types of chat services available

## Public Chat Service :
The service provides APIs to message all participants present in the meeting at once i.e. All the participants present in the meeting, will receive the message sent through the service.

**Note:** Whenever a new user joins or reconnection happens only the last 10 public messages are restored.

#### API :
`meetingService.publicChatService.sendMessage(message: String): Boolean` </br>

## Private Chat Service :
The service provides APIs to message individual participants present in the meeting i.e. Only the participant given as input to API will get the message. All participants present in the meeting may or may not
be eligible for a private chat. The service provides a list of eligible participants and only those participants will be available for a private chat.

**Note:** Whenever a participant disconnects and connects back in the same meeting, it is treated as a new user, and previous chat messages if any will not be retained.

#### API :
`meetingService.privateChatService.sendMessage(message: String, participant: Participant): Boolean`

There Rx Subscriptions are provided by each of the chat services to listen to the message and unread message count. Please refer to the API documentation for more details.

## ModeratorControlsService :
This is an in-meeting service that provides for moderator privileged controls. This is available only for a moderator.
`isModeratorControlsAvailable` can be checked for the service availability.

As a moderator, one can perform
- meeting recording start, stop
- mute, unmute participant's / all participants audio
- mute, unmute participant's / all participants video
- remove a participant from the meeting
- end the meeting for all immediately / after a certain delay
- spotlight a video participant : This is also referred to as "moderator pinning". Whenever a spotlight for a participant "X" is turned on by a moderator, people layout would be pushed to all the participants and the video of "X" will take the main stage irrespective of whoever is the dominant speaker. This can be used to spotlight self or any other video participant in the meeting. Spotlight is applicable only for video participants and not for audio only participants. 

Note that 
- ModeratorControlsService cannot override the local audio, video mute operations performed by the participants.
- Participants can locally override the mute enforcements by the ModeratorControlsService

Please refer to dokka documentation for details on the API set and the corresponding observables.

## ClosedCaptioningService :
Closed Captioning is the ability to provide text for the words spoken in meetings. BlueJeans is providing means for the meeting participant to turn-on or turn-off Closed Captioning while in the meeting. Please note that Closed Captioning setting should be enabled on your meeting. As of now, only english language is supported.

#### API :
- `startClosedCaptioning()`
- `stopClosedCaptioning()`

#### Observables with Closed Captioning feature :
- `isClosedCaptioningAvailable` provides availability of the closed captioning feature. Feature can be enabled or disabled at the scheduling options or the meeting feature associated with the account.
- `closedCaptionText` provides closed captioning text.
- `closedCaptioningState` provides the current state of closed captioning, can be started or stopped or null (when not in meeting)

## ModeratorWaitingRoomService :
This is an in-meeting service that provides waiting room controls. This is available only for a moderator.
`isWaitingRoomCapable` can be checked to see if the meeting supports waiting room or not.

As a moderator, one can perform
- turn waiting room on/off
- admit a waiting room participant into the meeting
- add all waiting room participants into the meeting
- deny a waiting room participant from joining a meeting
- deny all waiting room participants from joining a meeting
- demote an in-meeting participant to the waiting room

Note that
- An in-meeting participant can be demoted only if the waiting room is turned on.

Please refer to dokka documentation for details on the API set and the corresponding observables.

## Logging Service :
Uploads logs stored at internal app storage to BlueJeans SDK internal log server. The API takes user comments and the user name.
The name of the user serves as a unique identifier for us to identify the logs uploaded.

#### API :
`uploadLog(comments: String, username: String): Single<LogUploadResult>`

#### Single Result :
AlreadyUploading, Success, Failed

Logging Service also provides for API to generate audio capture dumps,which use to report any audio related issues such as no audio, low audio, echo, etc.
The audio capture dump created will be placed inside the internal app directory and uploaded to BlueJeans log server and the app logs, on calling the `uploadLog` API. Once the upload is successful, the dump will be cleared from the internal memory.
Note that the audio capture dumps will be heavier in size and use it only when needed, ensure to capture for 30 sec while u reproduce the issue and
upload them once captured.

#### API :
`enableAudioCaptureDump(enable: Boolean)`

## Subscriptions (ObservableValue and Rx Single's) :

#### RxSingle : 
This is a standard [Rx Single](http://reactivex.io/RxJava/javadoc/io/reactivex/Single.html)

#### ObservableValue :

Most of our subscriptions are stateful members called ObservableValues. 
These are our BJN custom reactive stream elements carrying a value that can be accessed (READ only) at any point of time and also allows a subscription. Through ObservableValue you can also access [RxObservable](http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html) and subscribe.

Sample app depicts the usage of both the RxSingle and ObeservableValue

#### ObservableEvent :

Unlike state observables, these wont carry a value that can accessed anytime. These are Fire and forget events which can mainly be used for notification banners and are generally provided in addition to the stateful variables and are to be used if necessary. These will be of type plain rx observable where u can use subscribeOn and ObserveOn from Rx library.


## SDK Sample Application :
We have bundled two sample apps in this repo. One for Java and another for kotlin.
It showcases the integration of BlueJeans SDK for permission flow and joins the flow. They have got a basic UI functionality and orientation support.

## Tracking & Analytics :
BlueJeans collects data from app clients who integrate with SDK to join BlueJeans meetings like Device information (ID, OS, etc.), Location, and usage data.

## Contributing :
The BlueJeans Android Client SDK is closed source and proprietary. As a result, we cannot accept pull requests. However, we enthusiastically welcome feedback on how to make our SDK better. If you think you have found a bug, or have an improvement or feature request, please file a GitHub issue and we will get back to you. Thanks in advance for your help!

## License : 
Copyright © 2022 BlueJeans Network. All usage of the SDK is subject to the Developer Agreement that can be found [here](LICENSE). Download the agreement and send an email to api-sdk@bluejeans.com with a signed version of this agreement, before any commercial or public facing usage of this SDK.

## 3<sup>rd</sup> party licenses :
Android Client SDK uses several open-source libraries. The document listing all the third-party libraries can be found [here](LICENSE-3RD-PARTY.txt).

## Legal Requirements :
Use of this SDK is subject to our [Terms & Conditions](https://www.bluejeans.com/terms-and-conditions-may-2020) and [Privacy Policy](https://www.bluejeans.com/privacy-policy). 
