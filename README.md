# sponge - A website crawler and links/images downloader command line tool
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
  -e, --file-extension TEXT       Extensions to download (example: png)
  -d, --depth INT                 Search depth (default: 1)
  -s, --include-subdomains        Include subdomains (default: false)
  -R, --concurrent-requests INT   Concurrent requests (default: 1)
  -D, --concurrent-downloads INT  Concurrent downloads (default: 1)
  -v, --version                   Show the version and exit
  -h, --help                      Show this message and exit
~~~
### Examples
~~~
./sponge -u https://www.gutenberg.org/ebooks/search/?sort_order=release_date \
         -o output \
         -t application/pdf \
         -t text/plain \
         -d 2 \
         -R 5 \
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
~~~
./sponge -u https://www.pexels.com/public-domain-images/ \
         -o output \
         -e jpeg \
         -e svg \
         -R 5 \
         -D 5 \
         -s
...
﹫ https://pexels.com/sk-sk/public-domain-images/
﹫ https://pexels.com/nb-no/public-domain-images/
﹫ https://pexels.com/tr-tr/public-domain-images/
﹫ https://pexels.com/privacy-policy/
⬇ /home/spypunk/output/pexels-photo-2923402.jpeg [45 KB]
⬇ /home/spypunk/output/marcelo-issa-612.jpeg [4 KB]
⬇ /home/spypunk/output/pexels-photo-3233372.jpeg [76 KB]
﹫ https://pexels.com/ru-ru/public-domain-images/
⬇ /home/spypunk/output/avatar_default-ab90ed807baa930476cb5abc4b547f7190f19fb418dc3581e686d5d418a611a1.svg [609 bytes]
⬇ /home/spypunk/output/pexels-photo-3297502.jpeg [17 KB]
⬇ /home/spypunk/output/pexels-photo-3354675.jpeg [11 KB]
⬇ /home/spypunk/output/favorite-f721c3d387889d5c3a9e0943c1836840a2954b9bebab846ca963877afee48f21.svg [425 bytes]
⬇ /home/spypunk/output/pexels-photo-3214726.jpeg [41 KB]
⬇ /home/spypunk/output/star-1bf7ee8c305832829a0a1e0b5c5d901e34e6732cd67c90715cd9b554a785877b.svg [387 bytes]
⬇ /home/spypunk/output/trace-hudson-652.jpeg [2 KB]
⬇ /home/spypunk/output/matheus-viana-985.jpeg [4 KB]
⬇ /home/spypunk/output/daria-shevtsova-592.jpeg [5 KB]
⬇ /home/spypunk/output/pexels-photo-3354641.jpeg [35 KB]
⬇ /home/spypunk/output/pexels-photo-2365465.jpeg [36 KB]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-220x1601.png)](http://www.wtfpl.net/)

Copyright © 2019 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
