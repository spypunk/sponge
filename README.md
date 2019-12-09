# sponge - A website crawler and links downloader command line tool
[![Build Status](https://travis-ci.org/spypunk/sponge.svg?branch=master)](https://travis-ci.org/spypunk/sponge)
[![License](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net/)
[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow)](https://twitter.com/spypunkk)
## How to build and run it?
You will need a Java JDK 8+ and maven 3+.
~~~
mvn clean package assembly:single

cd target && unzip sponge-0.1-SNAPSHOT.zip && cd sponge-0.1-SNAPSHOT

./sponge [OPTIONS]
~~~
## How to use it?
~~~
Usage: sponge [OPTIONS]

Options:
  -u, --uri VALUE           URI (example: https://www.google.com)
  -o, --output PATH         Output directory where files are downloaded
  -t, --mime-type TEXT      Mime types to download (example: text/plain)
  -d, --depth INT           Search depth (default: 1)
  -s, --include-subdomains  Include subdomains
  -h, --help                Show this message and exit
~~~
~~~
./sponge -u https://www.gutenberg.org/ebooks/search/%3Fsort_order%3Drelease_date \
         -o output \
         -t application/pdf \
         -t text/plain \
         -d 4

...
﹫ https://www.gutenberg.org/ebooks/search/?sort_order=release_date
﹫ https://www.gutenberg.org/ebooks/search/?sort_order=downloads
﹫ https://www.gutenberg.org/ebooks/search/?sort_order=title
﹫ https://www.gutenberg.org/ebooks/search/?sort_order=title&start_index=26
﹫ https://www.gutenberg.org/ebooks/50921
⬇ /home/spypunk/output/50921.txt.utf-8 [304 bytes]
﹫ https://www.gutenberg.org/ebooks/42474
⬇ /home/spypunk/output/42474-0.txt [217 KB]
﹫ https://www.gutenberg.org/ebooks/56796
⬇ /home/spypunk/output/56796-0.txt [336 KB]
﹫ https://www.gutenberg.org/ebooks/14403
⬇ /home/spypunk/output/14403.txt.utf-8 [304 bytes]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-220x1601.png)](http://www.wtfpl.net/)

Copyright © 2019 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.