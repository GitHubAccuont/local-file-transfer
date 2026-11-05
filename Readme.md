# LAN p2p File transfer and chat
___
This is basic app providing some casual utilities for the user allowing to transfer files and send
messages across the network. There might be many similar apps you may find, but this one is mine.
App made without any dependencies (Aside the maven plugin, which could in theory be done with a script; 
There are also Junit dependencies, but they are limited in scope and should not affect the final results of packaging).


___
## Usage
The app UI is pretty simple and consists of two sections: left section is chat with send button and
right section is connection panel with connection setting, discovery panel and select/transfer buttons. 
Those are self-explanatory and instead i will just add that any messaging/feedback from app is done with the
format of author:message where ``"remote" "me" "system"``. App only has barebones chat made with simple JTextArea,
since as its implied it is simple LAN transfer app and focusing on adding chat was not in priority.

### Connection
If there is another instance of app running within same network with different device you should be able to see it 
at the right panel around the center, where its name will be set. Clicking any of showed up addresses will attempt
to create a connection, and you will receive notes about successful connection from outside if you are not one starting it.
To disconnect simply click button that says so or click other device in discovery panel, it should change your connection aswell.

### Sending files/messages
To send message simply type it in chat tab and press button. If you are connected to any device, that is, otherwise UI is
supposedly disabled for that. For file transfer, be mindful as only **FILES** are accepted (Any type, extension e.t.c. just not folders).
Also do not disconnect mid transfer as it will cancel any ongoing file deliveries. Files sent do have some safety checks to prevent 
inserting pathways in filename to send it to other places (check [ReceiveUtils](src/main/java/org/example/transfer/utils/ReceiveUtils.java) for more info)

### Important notes
Some of important notes how app works for those who are interested:
* File transfer made in [Frame](src/main/java/org/example/transfer/dto/frame/Frame.java) which aside from its definitions
happen in payloads with default size of 256 KBs, which are not configurable by default but if anything you can just modify code to fix that
  (use the class attached enum to check for how frames are pressed)
* File transfers does create temporary file before full transfer happens, upon errors like connection lost and other happening, the said file is deleted and transfer gets canceled
* The files are stored in directory `($user.home).resolve(received)` which implies that if you are on windows it will be at
`%CurrentUser%/received`. I might however later add new arguments/ or other UI inputs to allow manual selection for where files should go (note will be deleted in that case).

### Arguments
The arguments app takes are completely optional but if you want, you can specify TCP port for running server instance
and your name as first two arguments to running , otherwise app defaults to ``9000`` and ``LAN Transferrer instance``. 
Remember to use it as `java -jar app.jar 9000 "My Name"` if you plan to use multiword name since the any sentence with
spacing is considered as multiple args.



## Limitations
Of course as a simple app this one doesnt
lack some of its own downsides, which include following:
* One active connection at a Time.
* No cancel mid transfer feature.
* There is no prompts or any callbacks to whether user wants to accept file/message, but once again it is a LAN app and not
serverside app that needs extra measures to protect against ill intentions
* No meaningful way to test discovery of other devices by using one device (The WSL would not 
solve anything as the lookup calls same port for looking into and one port for hosting server;
generally speaking app should work fine as-is from just the jar file on different devices,
but it would prove difficult if done so on one device.)

## Other notes
There is Junit tests written for most utilities classes aswell as somewhat extended description inside those classses,
but it still might cause confusion and if that is the case i suggest you start with the description. There isnt much else
to add aside from that tests might not cover all the cases and some important features, since
there was no meaningful way to test the discovery within same device, two instances or any good way to cover it with tests
 (previously there was attempt to add the cast to local addresses group using the multicast socket instead, but that would
imply that network you start this over does have any administrative group built in, which i can't guarantee).

## Afterword
If you find this repository interesting or helpful, feel free to download it and test for yourself. There will likely be 
no release added, so just feel free to package it into jar with inbuilt pom file using `maven package` command or using jpackage
if you want it as standalone app without any java dependency. In case you will be running it wtih java, the version was set to 11, 
so it will work on anything at that level or above.

