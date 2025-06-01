## 48sx CHANGELOG

* 1 Jun 2025 (Version 15.1.29146646)
  - Bug fixes for android 15 to optout EdgeToEdge
  - Intended to work from Android 5 to 15 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - In recent versions of Android, you must grant the 'Photos and Videos' permission
    for 'Load object on stack' and 'Save/Restore checkpoint ZIP' to function properly.
    The file must be located in the Download folder and have a '.png' extension.
    I hope it will be OK for Google Play!

* 28 May 2025 (Version 15.1.29140903)
  - Bug fixes and performance improvements
  - Intended to work from Android 5 to 15 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - In recent versions of Android, you must grant the 'Photos and Videos' permission
    for 'Load object on stack' and 'Save/Restore checkpoint ZIP' to function properly.
    The file must be located in the Download folder and have a '.png' extension.
    I hope it will be OK for Google Play!

* 15 Aug 2024 (Version 14.1.28728418)
  - Bug fixes and performance improvements
  - Intended to work from Android 5 to 14 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - In recent versions of Android, you must grant the 'Photos and Videos' permission
    for 'Load object on stack' and 'Save/Restore checkpoint ZIP' to function properly.
    The file must be located in the Download folder and have a '.png' extension.

* 11 Feb 2024 (Version 13.1.28459983)
  - Bug fixes and performance improvements
  - Intended to work from Android 4.4 to 13 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - On Android 13 (API 33), you must grant the 'Photos and Videos' permission
    for 'Load object on stack' and 'Save/Restore checkpoint ZIP' to function properly.
    The file must be located in the Download folder and have a '.png' extension.

* 26 Aug 2023 (Version 2.42.28217746)
  - Bug fixes and performance improvements
  - Intended to work from Android 4.4 to 13 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - New name for checkpoint ZIP: Download/48sx_cp_$DATE.zip
  - New SVG icon for Android above Oreo (Android 8, API >= 26)

* 10 Jul 2022 (Version 2.42.27624074)
  - Bug fixes and performance improvements
  - Intended to work from Android 4.4 to 12 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - Fixed the "Large LCD" option in the settings, and replaced it with "HP48 LCD" to
    get full pixels
  - The HP48's LCD screen has now a maximum width on your device, but you can set
    "HP48 LCD" in the settings get full pixels

* 4 June 2022 (Version 2.42.27573940)
  - Bug fixes and performance improvements
  - Intended to work from Android 4.4 to 12 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - Haptic feedback enabled by default, but can be disabled on settings
  - New menu: 'Save checkpoint ZIP As...' : save the current checkpoint in a zipped
    file (in Download/checkpoint_$DATE.zip). This zip file must contain the files
    ('hp48', 'rom', 'ram' and maybe 'port1' or 'port2')
  - New menu: 'Restore checkpoint ZIP' : restore from a zipped checkpoint.zip
    (that you choose)
  - Permissions on "Choose File" works on some Android 11 and 12, depending on the
    brand. This will be my next update...
  - When the action bar is enabled, pressing an hp48 button disables it again
  - Checkpoint is saved at the first startup and new functions to load a stuck HP48
  - New shortcuts, one for full reset, and another to restore from a checkpoint when
    the emulator is stuck

* 18 Oct 2019 (Version 1.42)
  - Bug fixes and performance improvements
  - Intended to work from Android 2.2 to 10 on armeabi-v7a, arm64-v8a, x86 and x86_64
    architectures
  - New skin, A..F are the same size of others key
  - Portrait mode only
  - New round icon
  - New name : 48sx

* 30 Jan 2016 (Version 1.39)
  - Intended to work from android 2.2 to 6.x on armeabi, armeabi-v7a, arm64-v8a, mips,
    mips64, x86 and x86_64 architectures
  - New amazing skin, should be more readable on big screens
  - New action bar with icons and menu when touching HP48sx screen
  - New icon
  - Complete fullscreen for android 4.4, 5.x and 6.x
  - Corrected port2 settings
  - Haptic feedback disabled by default, but can be re-enabled

* 26 Jan 2014  (Version 1.38)
  - Corrected "put program on stack" bug
  - Corrected FC bug on android 4.3
  - Corrected random freeze on android 4.4
  - Data files are now stored on /sdcard/Android/data/org.czo.droid48sx/files
  - Menu is displayed when touching HP screen
  - New menu: Save checkpoint (saves a backup copy of hp48/ram/rom/port1/port2)
  - New menu: Restore checkpoint (restores the backup copy of hp48/ram/rom/port1/port2)
  - New menu: Full reset (deletes firmware/ram and deactivate ports, if calc is still hung,
    please uninstall/reinstall)

* 7 Nov 2012 (Version 1.36)
  - first release

> Olivier Sirol <czodroid@gmail.com>
