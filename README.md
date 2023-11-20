sponge - A website crawler and links downloader command line tool [![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin) [![License](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net/)
===

## How to build and run it?

- Java JDK 11+
- Maven 3.6.3+

~~~
mvn clean package assembly:single

cd target && unzip sponge-X.X-SNAPSHOT.zip && cd sponge-X.X-SNAPSHOT

./sponge [OPTIONS]
~~~

## How to use it?

~~~
./sponge -h

Usage: sponge [OPTIONS]

Options:
  -u, --uri VALUE                 URI (example: https://www.google.com)
  -o, --output PATH               Output directory where files are downloaded
  -t, --mime-type TEXT            Mime types to download (example: text/plain)
  -e, --file-extension TEXT       Extensions to download (example: png)
  -d, --depth INT                 Search depth (default: 1)
  -m, --max-uris INT              Maximum uris to visit (default: 1000000)
  -s, --include-subdomains        Include subdomains
  -R, --concurrent-requests INT   Concurrent requests (default: 1)
  -D, --concurrent-downloads INT  Concurrent downloads (default: 1)
  -r, --referrer TEXT             Referrer (default: https://www.google.com)
  -U, --user-agent TEXT           User agent (default: Mozilla/5.0 (X11; Linux
                                  x86_64) AppleWebKit/537.36 (KHTML, like
                                  Gecko) Chrome/119.0.0.0 Safari/537.36)
  -O, --overwrite                 Overwrite existing files
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
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Checkie_Brown/hey/Checkie_Brown_-_09_-_Mary_Roose_CB_36.mp3 [8 MB] [4145.71 kB/s] [1/19]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_01_-_Brain_ID_1270.mp3 [8 MB] [4065.18 kB/s] [2/42]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_02_-_Brain_-_Instrumental_Retro_ID_1271.mp3 [8 MB] [4216.95 kB/s] [3/56]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_12_-_Madness_is_Everywhere_ID_1228.mp3 [9 MB] [4178.34 kB/s] [4/65]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_01_-_Allright_in_Lousiana_ID_1234.mp3 [8 MB] [3843.75 kB/s] [5/84]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_04_-_Peaceful_Morning_ID_1229.mp3 [9 MB] [2889.31 kB/s] [6/85]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_03_-_Spencer_-_Bluegrass_ID_1230.mp3 [9 MB] [3951.36 kB/s] [7/94]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/You_get_the_Blues_ID_1201.mp3 [10 MB] [3990.58 kB/s] [8/101]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Tropic_Island_-_Clearmix_ID_1172.mp3 [8 MB] [4146.20 kB/s] [9/101]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Traveling_Horse_ID_1207.mp3 [6 MB] [3411.93 kB/s] [10/101]
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
↓ /home/spypunk/output/www.gutenberg.org/files/61671/61671-0.txt [34 KB] [202.73 kB/s] [1/1]
↓ /home/spypunk/output/www.gutenberg.org/files/61673/61673-0.txt [363 KB] [778.25 kB/s] [2/3]
↓ /home/spypunk/output/www.gutenberg.org/files/61667/61667-0.txt [280 KB] [359.20 kB/s] [3/4]
↓ /home/spypunk/output/www.gutenberg.org/files/61672/61672-0.txt [953 KB] [149.74 kB/s] [4/4]
↓ /home/spypunk/output/www.gutenberg.org/files/61666/61666-0.txt [866 KB] [438.76 kB/s] [5/6]
↓ /home/spypunk/output/www.gutenberg.org/files/61662/61662-0.txt [556 KB] [625.44 kB/s] [6/6]
↓ /home/spypunk/output/www.gutenberg.org/files/61670/61670-0.txt [140 KB] [397.26 kB/s] [7/7]
↓ /home/spypunk/output/www.gutenberg.org/files/61665/61665-0.txt [74 KB] [277.85 kB/s] [8/8]
↓ /home/spypunk/output/www.gutenberg.org/ebooks/61664.txt.utf-8 [388 KB] [801.17 kB/s] [9/9]
↓ /home/spypunk/output/www.gutenberg.org/files/61661/61661-0.txt [142 KB] [397.52 kB/s] [10/10]
...
~~~

~~~
./sponge -u https://free-images.com/  \
         -o output \
         -e jpeg \
         -e jpg \
         -e png \
         -d 2 \
         -R 5 \
         -D 5 \
         -s
...
↓ /home/spypunk/output/free-images.com/sm/de66/bees_in_hive.jpg [24 KB] [11229.43 kB/s] [283/300]
↓ /home/spypunk/output/free-images.com/sm/602e/bees_on_flowers_collecting.jpg [15 KB] [70732.48 kB/s] [284/300]
↓ /home/spypunk/output/free-images.com/sm/7491/bees_on_yellow_flowers.jpg [14 KB] [54516.06 kB/s] [285/300]
↓ /home/spypunk/output/free-images.com/sm/e081/bees_pollenating_basil.jpg [15 KB] [57209.33 kB/s] [286/300]
↓ /home/spypunk/output/free-images.com/sm/7e8b/bees_pollenating_insects_bugs.jpg [13 KB] [52990.93 kB/s] [287/300]
↓ /home/spypunk/output/free-images.com/sm/a8ad/bees_pollen_insects_wings.jpg [7 KB] [17312.71 kB/s] [288/300]
↓ /home/spypunk/output/free-images.com/sm/9b0c/bees_really_like_pollinating_0.jpg [11 KB] [66770.38 kB/s] [289/300]
↓ /home/spypunk/output/free-images.com/sm/2d32/delicate_arch_utah_arches.jpg [8 KB] [44408.98 kB/s] [290/300]
↓ /home/spypunk/output/free-images.com/sm/bf50/landscape_arch.jpg [16 KB] [73487.99 kB/s] [291/300]
↓ /home/spypunk/output/free-images.com/sm/0f6a/golden_arches_omaha.jpg [10 KB] [65179.10 kB/s] [292/300]
...
~~~

## What about license?

This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-160x116.png)](http://www.wtfpl.net/)

Copyright © 2019-2023 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public
License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
