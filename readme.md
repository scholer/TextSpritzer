TextSpritzer
============

A TextView wrapper forked from [SpritzerTextView](https://github.com/andrewgiang/SpritzerTextView)

Note: This library has nothing to do with SpritzInc.

![SpritzerTextView example](http://i.imgur.com/mkeViYY.gif)

Differences include:

* A multithreaded design.
** Strings are parsed into words and delay is calculated in a separate thread, while already calculated words are being displayed.
* Ability to add strings so the end of the queue
* Ability to override the parsing function for custom delay calculation and word manipulation
** This allows you to add metadata to the string which can be parsed out and used to calculate a delay.
* Interface to register a callback when < x words are remaining to be displayed.
* Interface to add a metadata callback. Overwrite the parsing function and set an integer callback value on the parsed wordobject. Your frontend will be notified when that word is displayed.
* No sleeping threads.
** After each word is displayed, the display of the next word is scheduled using a ScheduledThreadPool.

See the included sample app for how to implement the various features.

License
------------
```
Copyright [2014] [Clint Armstrong]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```