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
  -o, --output PATH               Output directory where files are downloaded
  -t, --mime-type TEXT            Mime types to download (example: text/plain)
  -e, --file-extension TEXT       Extensions to download (example: png)
  -d, --depth INT                 Search depth (default: 1)
  -m, --max-uris INT              Maximum uris to process (default:
                                  2147483647)
  -s, --include-subdomains        Include subdomains
  -R, --concurrent-requests INT   Concurrent requests (default: 1)
  -D, --concurrent-downloads INT  Concurrent downloads (default: 1)
  -r, --referrer TEXT             Referrer (default: https://www.google.com)
  -U, --user-agent TEXT           User agent (default: Mozilla/5.0 (X11; Linux
                                  x86_64) AppleWebKit/537.36 (KHTML, like
                                  Gecko) Chrome/80.0.3987.122 Safari/537.36)
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
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Checkie_Brown/hey/Checkie_Brown_-_09_-_Mary_Roose_CB_36.mp3 [8 MB] [4035.29 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_01_-_Brain_ID_1270.mp3 [8 MB] [4291.79 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Not_my_Brain/Lobo_Loco_-_02_-_Brain_-_Instrumental_Retro_ID_1271.mp3 [8 MB] [4209.01 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_12_-_Madness_is_Everywhere_ID_1228.mp3 [9 MB] [4269.37 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_01_-_Allright_in_Lousiana_ID_1234.mp3 [8 MB] [4150.02 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_04_-_Peaceful_Morning_ID_1229.mp3 [9 MB] [3896.10 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Lobo_Loco/Salad_Mixed/Lobo_Loco_-_03_-_Spencer_-_Bluegrass_ID_1230.mp3 [9 MB] [4238.55 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/You_get_the_Blues_ID_1201.mp3 [10 MB] [4294.82 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Tropic_Island_-_Clearmix_ID_1172.mp3 [8 MB] [4311.11 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Traveling_Horse_ID_1207.mp3 [6 MB] [4321.62 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Sometimes_it_Rains_ID_1206.mp3 [11 MB] [4329.85 kB/s]
↓ /home/spypunk/output/files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/My_Yearnings/Seniorita_Bonita_ID_1194.mp3 [7 MB] [4319.61 kB/s]
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
↓ /home/spypunk/output/www.gutenberg.org/files/61579/61579-0.txt [66 KB] [249.41 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/ebooks/61578.txt.utf-8 [177 KB] [223.88 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/ebooks/61576.txt.utf-8 [170 KB] [261.94 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/files/61574/61574-0.txt [80 KB] [218.19 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/files/61573/61573-0.txt [256 KB] [163.96 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/files/61577/61577-0.txt [47 KB] [249.83 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/files/61575/61575-0.txt [239 KB] [210.76 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/ebooks/61571.txt.utf-8 [47 KB] [486.18 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/ebooks/61572.txt.utf-8 [905 KB] [252.70 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/ebooks/61570.txt.utf-8 [118 KB] [427.63 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/files/61568/61568-0.txt [175 KB] [188.35 kB/s]
↓ /home/spypunk/output/www.gutenberg.org/files/61569/61569-0.txt [121 KB] [183.77 kB/s]
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
↓ /home/spypunk/output/free-images.com/sm/96a3/violets_flowers_violet_background.jpg [8 KB] [76554.05 kB/s]
↓ /home/spypunk/output/free-images.com/sm/7b79/violet_red_clay_violet.jpg [9 KB] [57321.59 kB/s]
↓ /home/spypunk/output/free-images.com/sm/dd7c/violet_purple_viola_blue.jpg [21 KB] [15939.76 kB/s]
↓ /home/spypunk/output/free-images.com/sm/ac2a/violets_flowers_violet_nature.jpg [14 KB] [97087.82 kB/s]
↓ /home/spypunk/output/free-images.com/sm/c48c/violets_violet_florets_spring.jpg [16 KB] [113878.19 kB/s]
↓ /home/spypunk/output/free-images.com/sm/dda0/violet_leaf_leaf_green.jpg [18 KB] [105489.70 kB/s]
↓ /home/spypunk/output/free-images.com/sm/aa8f/violet_wild_violets_spring.jpg [21 KB] [147272.97 kB/s]
↓ /home/spypunk/output/free-images.com/sm/b3e0/violets_violet_flower_spring_0.jpg [12 KB] [83200.41 kB/s]
↓ /home/spypunk/output/free-images.com/sm/2c40/violets_violet_flower_spring.jpg [10 KB] [103418.85 kB/s]
↓ /home/spypunk/output/free-images.com/sm/228d/violet_blossom_bloom_violet.jpg [23 KB] [6465.70 kB/s]
↓ /home/spypunk/output/free-images.com/sm/67c3/violets_garden_flowers_violet.jpg [18 KB] [20872.61 kB/s]
↓ /home/spypunk/output/free-images.com/sm/d90f/violet_flowers_garden_plant.jpg [25 KB] [12348.48 kB/s]
↓ /home/spypunk/output/free-images.com/sm/20df/scented_violets_viola_odorata.jpg [25 KB] [20470.55 kB/s]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-160x116.png)](http://www.wtfpl.net/)

Copyright © 2019-2020 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
