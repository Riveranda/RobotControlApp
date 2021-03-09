# RobotControlApp
<h3>This project is a simple Android application, written in Kotlin, used to control a raspberry pi robot.</h3>
<h4> Server Sockets </h4>
Communication with the raspberry pi is done via a low level implementation of Kotlin's server sockets.</br>
These server sockets are contained within an Android Service, and feature auto-reconnect and error handling. </br>
With the proper setup, sever sockets allow one to communicate over local networks with ease, and even over</br>
broader internet. </br>
<h4> User Interface </h4>
The UI was designed to be extremely simple, and obfuscate the user from the complicated backend.</br>
The official colors of MSU, blue and gold were used to add some team spirit.</br>
</br>
![User Interface](uiscreenshot.png?raw=true "UiScreenshot")</br>
<h4>Driven by Python</h4>
On the other end of the server socket, was a python program containing a simple server socket. From here</br>
commands are read and sent to another python thread which manages the robot's motor controllers.  
