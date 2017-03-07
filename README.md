# Monitor

Heart rate monitor sample application built for the [Mason Platform](https://bymason.com/)

### Cloning

`git clone https://github.com/MasonAmerica/Monitor.git`

a directory `Monitor` will be created for this project

### Building

To deploy your APK to the Mason Platform a **signed APK is required**

- **Signed APK**
    - In Android Studio: *Build > Generate Signed APK*
    ([Additional info in **Sign your release build**](https://developer.android.com/studio/publish/app-signing.html))

- Debug APK
    - In Android Studio: *Build > Build APK*
    - Terminal/Gradle:
        - mac/linux: `./gradlew build`
        - windows: `gradlew build`

### Deploying

To deploy your signed APK to your Mason device(s) you first need to register it to the Mason Platform
using the Mason CLI. Find detailed instructions on the Mason [intro](http://docs.bymason.com/intro/) and
[quick start page](http://docs.bymason.com/quick-start/).


0. Install the [Mason CLI](http://docs.bymason.com/getting-started/)
1. Login: `mason login`
2. Register your apk: `mason register apk app-release.apk`

Now you can deploy your apk!

`mason deploy apk com.iheartlives.monitor 1`

### Docs

http://docs.bymason.com/

or reach out at [support@bymason.com](mailto://support@bymason.com) if you have any questions or feedback
