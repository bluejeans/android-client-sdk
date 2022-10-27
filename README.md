[![BlueJeans Android Software Development Kit](https://user-images.githubusercontent.com/23289872/127987669-3842046b-2f08-46e4-9949-6bf0cdb45d95.png "BlueJeans Android Software Development Kit")](https://www.bluejeans.com "BlueJeans Android Software Development Kit")

# BlueJeans Android Client Software Development Kit

The BlueJeans Android Client Software Development Kit (SDK) gives a quick and easy way to bring an immersive video-calling experience into your android applications.

With BlueJeans Android Client SDK, participants can join video conference meetings where they receive individual video streams from each of the video participants in the meeting. This provides an enhanced remote video quality experience with the resolution, fps of individual streams better as compared to a single composited stream in an earlier hybrid model.

## Features :
- Audio, Video and Bluetooth Permission handling
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
- Moderator Controls
  - Meeting recording
  - Mute/UnMute Audio/Video of other participants / all participants
  - Remove a participant from the meeting
  - End meeting for all immediately or after a certain delay
- Audio capture dumps (debug facility)
- Waiting room
- Active Speaker
- Remote and Local mute information
- 720p video capture
- 720p video receive

## New Features :
- Individual video streams

## Current Version : 1.6.0

## Pre-requisites :
- **Android API level :** Min level 26

- **Android Device :**
  - OS level - Oreo 8.0 or later
  - CPU - armeabi-v7a, arm64-v8a
  - No support for emulator yet

- **Android Project & Gradle Settings:**
  - Android X
  - Compile SDK Version: 32 and above
  - Source and Target compatibility to java version 1_8 in gradle
  - RxJava, RxKotlin

## Developer Portal :
Detailed documentation of SDK at our [developer portal](https://docs.bluejeans.com/Android_SDK/Overview.htm)

## SDK API Documentation :
Detailed documentation of SDK functions is available [here](https://bluejeans.github.io/android-client-sdk)

## How it all works?
You can experience BlueJeans meetings using the android client SDK by following the below 2 steps -

### Generate a meeting ID :
As a prerequisite to using the BlueJeans Android Client SDK to join meetings, you need to have a BlueJeans meeting ID. If you do not have a meeting ID then you can create one using a meeting schedule option using a BlueJeans account as below
- Sign up for a BlueJeans Account either by opting in for a [trial](https://www.bluejeans.com/free-video-conferencing-trial) or a [paid mode](https://store.bluejeans.com/)
- Once the account is created, you can schedule a meeting either by using the account or through the [direct API](https://bluejeans.github.io/api-rest-howto/schedule.html) calls. In order to enable API calls on your account, please reach out to [support team](https://support.bluejeans.com/s/contactsupport).

### Integrate BlueJeans Android Client SDK
Integrate the SDK using the guidelines at our [developer portal](https://docs.bluejeans.com/Android_SDK/Overview.htm) and use SDK APIs to join a meeting using the generated meeting ID.

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
