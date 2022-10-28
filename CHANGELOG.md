Change Log
==========

Version 1.0.0-alpha.1 *(02-2021)*
---------------------------------
Features:
- Audio and Video Permission handling. 
- Join, End Meeting. 
- Self Video. 
- Remote Video, Remote Video states. 
- Content receive. 
- Audio and Video self mute. 
- Orientation handling. 
- Video device enumeration, Selection. 
- Audio device enumeration, Selection. 
- Video Layout switch.

Version 1.0.0-alpha.2 *(03-2021)*
---------------------------------
Features:
- Meeting participants list.
- Participant properties: Audio mute state, Video mute state, isSelf, Unique identifier and Name.
- Self Participant details.

Version 1.0.0-alpha.3 *(04-2021)*
---------------------------------
Features:
- Content Share.
- Log Upload.


Version 1.0.0-alpha.4 *(05-2021)*
---------------------------------
Features / Changes:

- Multi stream support (Sequin video layouts). We would receive individual remote streams in place of single composited video from server providing a better video experience with enhanced meeting layouts.
- RxJava upgraded from version 2.0 to version 3.0. If consumer app is already using RxJava2 then additionally RxJava3 should be added to consume BlueJeansSDK reactive properties.
- Removed setSelfVideoOrientation. SelfVideoFragment can now handle orientation changes on its own.
- BlueJeansSDKInitParams carries a new configuration parameter by name videoConfiguration which allows you to configure number of maximum participants in the Gallery Video Layout
- Fix for webRTC class conflict issue. Consumer app can now bundle webRTC libraries in addition to BlueJeans SDK
- Permission service now needs a registration to be done in the onCreate of an activity before requesting for permissions

Version 1.0.0-alpha.5 *(05-2021)*
---------------------------------
Features:

- Support for enabling torch / flash unit on a device
- Support to set capture requests such as zoom, exposure on the active video device
- Misc bug fixes


Version 1.0.0-alpha.6 *(06-2021)*
---------------------------------
Features:

- API re-architecture. APIs are grouped into several relevant services. See the image attached for architecture and API changes.
- Support for Private and Public Chat
- Support for Remote Video Mute and Content Mute, useful when app is put to background
- Kotlin Sample Application
- New Meeting State : Validating
- Misc bug fixes

Architecture:

![BlueJeansSDKArch](https://user-images.githubusercontent.com/23289872/123610017-cf63af80-d81d-11eb-998e-756ba4fdd6db.jpg)

API changes:

<img width="513" alt="APIChanges" src="https://user-images.githubusercontent.com/23289872/123609917-bc50df80-d81d-11eb-9442-1151c8760b3a.png">


Version 1.0.0-beta.1 *(07-2021)*
---------------------------------
Features / Changes:
- Meeting information(Title, Host, URL)
- Audio quality enhancements
- Misc bug fixes

Version 1.0.0 *(08-2021)*
---------------------------------
Features / Changes:
- Security fixes
- Sample application improvements
- Misc bug fixes

Version 1.1.0 *(09-2021)*
---------------------------------
Features / Changes:
- 720p video capture (Experimental API)
- Moderator Controls
	- Meeting recording
	- Mute/UnMute Audio/Video of other participants / all participants
	- Remove a participant from the meeting
	- End meeting for all immediately or after a certain delay
- Audio capture dumps (debug facility)

Version 1.2.0 *(09-2021)*
---------------------------------
Features / Changes:
- Closed captioning
- Spotlight participant video
- Misc bug fixes

Version 1.3.0 *(10-2021)*
---------------------------------
Features / Changes:
- Waiting Room support
- Added information of being a moderator or not with the Participant object
- Meeting ID and Participant Passcode properties with meeting information
- Misc bug fixes

Version 1.3.1 *(11-2021)*
---------------------------------
Features / Changes:
- Active speaker
- Mute incoming audio
- Remote mute and Local mute information
- Misc bug fixes

Version 1.3.2 *(02-2022)*
---------------------------------
Features / Changes:
- Enhanced participant mute states to reflect whether mute was triggered locally or remotely
- Migration from JCenter to Maven Central for internal 3rd party dependencies
- Misc bug fixes

Version 1.4.0 *(06-2022)*
---------------------------------
Features / Changes:
- Target Android SDK level 30
- Misc bug fixes


Version 1.4.1 *(06-2022)*
---------------------------------
Features / Changes:
- Misc bug fixes

Version 1.5.0 *(07-2022)*
---------------------------------
Features / Changes:
- 720p video send and recieve
- Target OS level 12
- Misc bug fixes

Version 1.6.0 *(10-2022)*
---------------------------------
Features / Changes:
### 🚨 ***Breaking Changes*** 

- videoDeviceService.enableSelfVideoPreview is no longer available. We recommend you to use meetingService.setVideoMuted instead
- meetingService.setAudioMuted, and meetingService.setVideoMuted may now be set prior to joining a meeting,
or while in the waiting room. Any updates to the localMuted state will be retained across SDK lifecycle.

### New License
See the updated LICENSE

### Features
#### Individual Stream Control
- Create custom layouts and set the VideoLayout to `Custom`
- Request specific streams of video with different configurations of your choice using the `VideoStreamService`
- Render videos with different styles in your own custom sized views with `attachParticipantStreamToView`
