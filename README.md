sponge - A website crawler and links downloader command line tool [![Build Status](https://travis-ci.org/spypunk/sponge.svg?branch=master)](https://travis-ci.org/spypunk/sponge) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d82ffffb736c4d82858a63385a6f900a)](https://www.codacy.com/manual/spypunk/sponge?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=spypunk/sponge&amp;utm_campaign=Badge_Grade) [![Download sponge](https://img.shields.io/sourceforge/dt/spypunk-sponge.svg)](https://sourceforge.net/projects/spypunk-sponge/files/latest/download) [![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin) [![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow)](https://twitter.com/spypunkk) [![License](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net/)
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
  -m, --max-uris INT              Maximum uris to process (default: 2147483647)
  -s, --include-subdomains        Include subdomains
  -R, --concurrent-requests INT   Concurrent requests (default: 1)
  -D, --concurrent-downloads INT  Concurrent downloads (default: 1)
  -r, --referrer TEXT             Referrer (default: https://www.google.com)
  -U, --user-agent TEXT           User agent (default: Mozilla/5.0 (X11; Linux
                                  x86_64) AppleWebKit/537.36 (KHTML, like
                                  Gecko) Chrome/79.0.3945.88 Safari/537.36)
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
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_01_-_Brain_ID_1270.mp3 [8 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_02_-_Brain_-_Instrumental_Retro_ID_1271.mp3 [8 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_12_-_Madness_is_Everywhere_ID_1228.mp3 [9 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_01_-_Allright_in_Lousiana_ID_1234.mp3 [8 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_04_-_Peaceful_Morning_ID_1229.mp3 [9 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_03_-_Spencer_-_Bluegrass_ID_1230.mp3 [9 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/You_get_the_Blues_ID_1201.mp3 [10 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Traveling_Horse_ID_1207.mp3 [6 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Tropic_Island_-_Clearmix_ID_1172.mp3 [8 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Seniorita_Bonita_ID_1194.mp3 [7 MB]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Remember_Love_ID_1166.mp3 [9 MB]
...
~~~
~~~
./sponge -u https://www.gutenberg.org/ebooks/search/?sort_order=release_date \
         -o output \
         -t text/plain \
         -d 2 \
         -R 5 \
         -D 5
...
↓ /home/spypunk/output/gutenberg.org/files/61012/61012-0.txt [149 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61011.txt.utf-8 [192 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61013.txt.utf-8 [44 KB]
↓ /home/spypunk/output/gutenberg.org/files/61010/61010-0.txt [302 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61009.txt.utf-8 [36 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61008.txt.utf-8 [471 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61007.txt.utf-8 [38 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61006.txt.utf-8 [47 KB]
↓ /home/spypunk/output/gutenberg.org/files/61004/61004-0.txt [250 KB]
↓ /home/spypunk/output/gutenberg.org/files/61003/61003-0.txt [337 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/61002.txt.utf-8 [74 KB]
↓ /home/spypunk/output/gutenberg.org/ebooks/60999.txt.utf-8 [25 KB]
↓ /home/spypunk/output/gutenberg.org/files/60998/60998-0.txt [124 KB]
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
↓ /home/spypunk/output/images.pexels.com/photos/3393375/pexels-photo-3393375.jpeg [37 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1791804/-621.jpeg [5 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3397935/pexels-photo-3397935.jpeg [69 KB]
↓ /home/spypunk/output/images.pexels.com/photos/1927314/pexels-photo-1927314.jpeg [62 KB]
↓ /home/spypunk/output/images.pexels.com/photos/245535/pexels-photo-245535.jpeg [51 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1737150/eternal-happiness-197.jpeg [4 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3326363/pexels-photo-3326363.jpeg [22 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1787606/zack-melhus-241.jpeg [5 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3363341/pexels-photo-3363341.jpeg [63 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/193704/print-proper-419.jpeg [4 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/220024/daria-shevtsova-592.jpeg [5 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3326103/pexels-photo-3326103.jpeg [26 KB]
↓ /home/spypunk/output/images.pexels.com/photos/2085376/pexels-photo-2085376.jpeg [51 KB]
↓ /home/spypunk/output/images.pexels.com/photos/3377538/pexels-photo-3377538.jpeg [20 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/372053/matt-hardy-504.jpeg [5 KB]
↓ /home/spypunk/output/images.pexels.com/photos/2822949/pexels-photo-2822949.jpeg [10 KB]
↓ /home/spypunk/output/images.pexels.com/photos/1953451/pexels-photo-1953451.jpeg [40 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/1112138/emre-kuzu-719.jpeg [1 KB]
↓ /home/spypunk/output/images.pexels.com/users/avatars/927188/milena-santos-885.jpeg [4 KB]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-160x116.png)](http://www.wtfpl.net/)

Copyright © 2019-2020 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
