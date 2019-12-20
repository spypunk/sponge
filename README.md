sponge - A website crawler and links downloader command line tool [![Build Status](https://travis-ci.org/spypunk/sponge.svg?branch=master)](https://travis-ci.org/spypunk/sponge) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d82ffffb736c4d82858a63385a6f900a)](https://www.codacy.com/manual/spypunk/sponge?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=spypunk/sponge&amp;utm_campaign=Badge_Grade) [![Download sponge](https://img.shields.io/sourceforge/dt/spypunk-sponge.svg)](https://sourceforge.net/projects/spypunk-sponge/files/latest/download) [![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow)](https://twitter.com/spypunkk) [![License](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net/)
===
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
  -s, --include-subdomains        Include subdomains
  -R, --concurrent-requests INT   Concurrent requests (default: 1)
  -D, --concurrent-downloads INT  Concurrent downloads (default: 1)
  -v, --version                   Show the version and exit
  -h, --help                      Show this message and exit
~~~
### Examples
~~~
./sponge -u https://freemusicarchive.org/genre/Blues \
         -o output \
         -e mp3 \
         -d 2 \
         -R 5 \
         -s
...
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Dream_your_Dreams_ID_1195
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Do_not_forget_me_ID_1028
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_01_-_Brain_ID_1270.mp3 [8 MB]
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Arround_the_Cliffs_ID_1202
↺ https://freemusicarchive.org/music/Rose_City_Kings/Live_at_KBOO_for_Blues_Junction_08132016/Rose_City_Kings-Aug_2016-LIVE
↺ https://freemusicarchive.org/music/Punk_Rock_Opera/Punk_Rock_Opera_Vol_II/Punk_Rock_Opera_-_Punk_Rock_Opera_Vol_II_Album_-_14_1945
↺ https://freemusicarchive.org/music/Rose_City_Kings/
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_02_-_Brain_-_Instrumental_Retro_ID_1271.mp3 [8 MB]
↺ https://freemusicarchive.org/music/Rose_City_Kings/Live_at_KBOO_for_Blues_Junction_08132016/
↺ https://freemusicarchive.org/music/Punk_Rock_Opera/
↺ https://freemusicarchive.org/music/Punk_Rock_Opera/Punk_Rock_Opera_Vol_II/
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_12_-_Madness_is_Everywhere_ID_1228.mp3 [9 MB]
↺ https://freemusicarchive.org/genre/Blues?sort=track_date_published&d=1&page=2
↺ https://freemusicarchive.org/genre/Blues?sort=track_date_published&d=1&page=3
↺ https://freemusicarchive.org/genre/Blues?sort=track_date_published&d=1&page=4
...
~~~
~~~
./sponge -u https://www.gutenberg.org/ebooks/search/?sort_order=release_date \
         -o output \
         -t application/pdf \
         -t text/plain \
         -d 2 \
         -R 5 \
         -D 5
...
↺ https://gutenberg.org/terms_of_use/
↺ https://gutenberg.org/
↺ https://gutenberg.org/ebooks/
↓ /home/spypunk/output/gutenberg.org/ebooks/60982.txt.utf-8 [40 KB]
↺ https://gutenberg.org/ebooks/author/1660
↺ https://gutenberg.org/ebooks/60980/also/
↺ https://gutenberg.org/wiki/Gutenberg:Help_on_Bibliographic_Record_Page
↺ https://gutenberg.org/ebooks/send/megaupload/60980.html
↺ https://gutenberg.org/files/60980/60980-h/60980-h.htm
↺ https://gutenberg.org/files/60980/
↺ https://gutenberg.org/wiki/Main_Page
↺ https://gutenberg.org/wiki/Gutenberg:Project_Gutenberg_Needs_Your_Donation
↓ /home/spypunk/output/gutenberg.org/files/60980/60980-0.txt [25 KB]
...
~~~
~~~
./sponge -u https://www.pexels.com/public-domain-images/ \
         -o output \
         -e jpeg \
         -d 3 \
         -R 5 \
         -D 5 \
         -s
...
↺ https://pexels.com/ro-ro/public-domain-images/
↺ https://pexels.com/nb-no/public-domain-images/
↺ https://pexels.com/ru-ru/public-domain-images/
↺ https://pexels.com/sk-sk/public-domain-images/
↺ https://pexels.com/tr-tr/public-domain-images/
↺ https://pexels.com/privacy-policy/
↓ /home/spypunk/output/images.pexels.com/photos/2868847/pexels-photo-2868847.jpeg [17 KB]
↓ /home/spypunk/output/images.pexels.com/photos/948331/pexels-photo-948331.jpeg [74 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1490165/daniel-mingook-kim-308.jpeg [5 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3351676/pexels-photo-3351676.jpeg [55 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/671805/su-su-721.jpeg [4 KB]
↓ /home/spypunk/output/images.pexels.com/photos/2318555/pexels-photo-2318555.jpeg [56 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3234167/pexels-photo-3234167.jpeg [72 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1409183/firaaz-hisyari-267.jpeg [5 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1452844/marcelo-issa-612.jpeg [4 KB]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-160x116.png)](http://www.wtfpl.net/)

Copyright © 2019 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
