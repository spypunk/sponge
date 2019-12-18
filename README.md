sponge - A website crawler and links/images downloader command line tool [![Build Status](https://travis-ci.org/spypunk/sponge.svg?branch=master)](https://travis-ci.org/spypunk/sponge) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d82ffffb736c4d82858a63385a6f900a)](https://www.codacy.com/manual/spypunk/sponge?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=spypunk/sponge&amp;utm_campaign=Badge_Grade) [![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow)](https://twitter.com/spypunkk) [![License](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net/)
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
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Morning_at_the_Creek_ID_1201
⇩ https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Ending_at_the_Trainstation_ID_1994.mp3
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Ending_at_the_Trainstation_ID_1994
⇩ https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Dream_your_Dreams_ID_1195.mp3
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Dream_your_Dreams_ID_1195
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Do_not_forget_me_ID_1028
⇩ https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Do_not_forget_me_ID_1028.mp3
⇩ https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Arround_the_Cliffs_ID_1202.mp3
↺ https://freemusicarchive.org/music/Lobo_Loco/My_Yearnings/Arround_the_Cliffs_ID_1202
⇩ https://files.freemusicarchive.org/storage-freemusicarchive-org/music/KBOO/rose_city/Live_at_KBOO_for_Blues_Junction_08132016/rose_city_-_01_-_Rose_City_Kings-Aug_2016-LIVE.mp3
↺ https://freemusicarchive.org/music/Rose_City_Kings/Live_at_KBOO_for_Blues_Junction_08132016/Rose_City_Kings-Aug_2016-LIVE
⬇ /home/spypunk/output/Checkie_Brown_-_09_-_Mary_Roose_CB_36.mp3 [8 MB]
↺ https://freemusicarchive.org/music/Rose_City_Kings/
↺ https://freemusicarchive.org/music/Punk_Rock_Opera/Punk_Rock_Opera_Vol_II/Punk_Rock_Opera_-_Punk_Rock_Opera_Vol_II_Album_-_14_1945
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
↺ https://gutenberg.org/ebooks/60920
↺ https://gutenberg.org/ebooks/60919
↺ https://gutenberg.org/ebooks/
↺ https://gutenberg.org/wiki/Main_Page
↺ https://gutenberg.org/wiki/Gutenberg:Project_Gutenberg_Needs_Your_Donation
↺ https://gutenberg.org/terms_of_use/
⇩ https://gutenberg.org/ebooks/60942.txt.utf-8
⬇ /home/spypunk/output/60942.txt.utf-8 [302 KB]
⇩ https://gutenberg.org/files/60943/60943-0.txt
⬇ /home/spypunk/output/60943-0.txt [505 KB]
⇩ https://gutenberg.org/files/60941/60941-0.txt
⬇ /home/spypunk/output/60941-0.txt [248 KB]
⇩ https://gutenberg.org/files/60938/60938-0.txt
⬇ /home/spypunk/output/60938-0.txt [696 KB]
⇩ https://gutenberg.org/files/60937/60937-0.txt
⬇ /home/spypunk/output/60937-0.txt [608 KB]
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
↺ https://pexels.com/public-domain-images/
⇩ https://images.pexels.com/photos/948331/pexels-photo-948331.jpeg?auto=compress&crop=focalpoint&cs=tinysrgb&fit=crop&h=350.0&sharp=40&w=1400
⇩ https://images.pexels.com/photos/2868847/pexels-photo-2868847.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500
⇩ https://pexels.com/assets/_svg/avatar_default-ab90ed807baa930476cb5abc4b547f7190f19fb418dc3581e686d5d418a611a1.svg
⇩ https://images.pexels.com/users/avatars/1490165/daniel-mingook-kim-308.jpeg?w=60&h=60&fit=crop&crop=faces
⇩ https://images.pexels.com/photos/3234167/pexels-photo-3234167.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500
⬇ /home/spypunk/output/pexels-photo-2868847.jpeg [17 KB]
⬇ /home/spypunk/output/pexels-photo-948331.jpeg [74 KB]
⇩ https://pexels.com/assets/favorite-f721c3d387889d5c3a9e0943c1836840a2954b9bebab846ca963877afee48f21.svg
⇩ https://images.pexels.com/photos/3351676/pexels-photo-3351676.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500
⇩ https://pexels.com/assets/star-1bf7ee8c305832829a0a1e0b5c5d901e34e6732cd67c90715cd9b554a785877b.svg
⇩ https://images.pexels.com/users/avatars/1051433/denis-perekhrest-261.jpeg?w=60&h=60&fit=crop&crop=faces
⬇ /home/spypunk/output/pexels-photo-3234167.jpeg [72 KB]
⬇ /home/spypunk/output/daniel-mingook-kim-308.jpeg [5 KB]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-160x116.png)](http://www.wtfpl.net/)

Copyright © 2019 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
