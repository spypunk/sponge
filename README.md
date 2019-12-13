# sponge - A website crawler and links downloader command line tool
[![Build Status](https://travis-ci.org/spypunk/sponge.svg?branch=master)](https://travis-ci.org/spypunk/sponge)
[![License](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net/)
[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow)](https://twitter.com/spypunkk)
## How to build and run it?
You will need a Java JDK 8+ and maven 3.3.9 or above.
~~~
mvn clean package assembly:single

cd target && unzip sponge-X.X-SNAPSHOT.zip && cd sponge-X.X-SNAPSHOT

./sponge [OPTIONS]
~~~
## How to use it?
~~~
Usage: sponge [OPTIONS]

Options:
  -u, --uri VALUE                 URI (example: https://www.google.com)
  -o, --output VALUE              Output directory where files are downloaded
  -t, --mime-type TEXT            Mime types to download (example: text/plain)
  -d, --depth INT                 Search depth (default: 1)
  -s, --include-subdomains        Include subdomains
  -R, --concurrent-requests INT   Concurrent requests (default: 1)
  -D, --concurrent-downloads INT  Concurrent downloads (default: 1)
  -h, --help                      Show this message and exit
~~~
~~~
./sponge -u https://www.gutenberg.org/ebooks/search/?sort_order=release_date \
         -o output \
         -t application/pdf \
         -t text/plain \
         -d 2
         -R 5
         -D 5
...
﹫ https://www.gutenberg.org/ebooks/send/dropbox/60892.kindle.images
﹫ https://www.gutenberg.org/ebooks/send/gdrive/60892.kindle.noimages
﹫ https://www.gutenberg.org/ebooks/send/msdrive/60892.kindle.images
﹫ https://www.gutenberg.org/ebooks/send/dropbox/60892.kindle.noimages
﹫ https://www.gutenberg.org/files/60892/
﹫ https://www.gutenberg.org/ebooks/send/msdrive/60892.kindle.noimages
⬇ /home/spypunk/output/60892.txt.utf-8 [304 bytes]
﹫ https://www.gutenberg.org/ebooks/search/?sort_order=random
﹫ https://www.gutenberg.org/ebooks/author/1351
﹫ https://www.gutenberg.org/ebooks/subject/138
﹫ https://www.gutenberg.org/ebooks/subject/3316
﹫ https://www.gutenberg.org/ebooks/subject/12294
﹫ https://www.gutenberg.org/ebooks/60891/also/
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-220x1601.png)](http://www.wtfpl.net/)

Copyright © 2019 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.