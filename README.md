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
↺ https://freemusicarchive.org/genre/Blues?sort=track_date_published&d=1&page=6
↺ https://freemusicarchive.org/genre/Blues?sort=track_date_published&d=1&page=109
↺ https://freemusicarchive.org/genre/Blues?sort=track_date_published&d=1&page=7
↓ /home/spypunk/output/You_get_the_Blues_ID_1201.mp3 [10 MB]
↺ https://freemusicarchive.org/Privacy_Policy
↺ https://freemusicarchive.org/Terms_of_Use
↓ /home/spypunk/output/Tropic_Island_-_Clearmix_ID_1172.mp3 [8 MB]
↺ https://freemusicarchive.org/music/Rose_City_Kings/Live_at_KBOO_for_Blues_Junction_08132016/
↓ /home/spypunk/output/Lobo_Loco_-_03_-_Spencer_-_Bluegrass_ID_1230.mp3 [9 MB]
↺ https://freemusicarchive.org/member/Alpha_Hydrae/Chill_and_ambiant_songs_from_Monplaisir__cies_projects_under_Creative_Commons_0_and_Public_Domain_li
↓ /home/spypunk/output/Traveling_Horse_ID_1207.mp3 [6 MB]
↺ https://freemusicarchive.org/music/Monplaisir/Heat_of_the_Summer/Monplaisir_-_Monplaisir_-_Heat_of_the_Summer_-_11_Estampe_Galactus_Barbare_Epaul_Giraffe_Ennui
↓ /home/spypunk/output/Sometimes_it_Rains_ID_1206.mp3 [11 MB]
↺ https://freemusicarchive.org/music/Monplaisir/Surtout_ne_pas_se_perdre_2011-2016/Monplaisir_-_Surtout_ne_pas_se_perdre_2011-2016_-_01_Everything_is_true
↓ /home/spypunk/output/Seniorita_Bonita_ID_1194.mp3 [7 MB]
↺ https://freemusicarchive.org/music/
↺ https://freemusicarchive.org/music/Monplaisir/
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
↺ https://gutenberg.org/ebooks/author/285
↺ https://gutenberg.org/ebooks/60948/also/
↺ https://gutenberg.org/ebooks/send/megaupload/60948.html
↓ /home/spypunk/output/60954-8.txt [236 KB]
↺ https://gutenberg.org/files/60948/60948-h/60948-h.htm
↺ https://gutenberg.org/files/60948/
↺ https://gutenberg.org/ebooks/author/51202
↺ https://gutenberg.org/ebooks/60945/also/
↺ https://gutenberg.org/browse/languages/it
↺ https://gutenberg.org/ebooks/send/megaupload/60945.html
↓ /home/spypunk/output/60948.txt.utf-8 [352 KB]
↺ https://gutenberg.org/files/60945/
↺ https://gutenberg.org/ebooks/author/46334
↺ https://gutenberg.org/ebooks/60947/also/
↺ https://gutenberg.org/ebooks/send/megaupload/60947.html
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
↺ https://pexels.com/el-gr/search/%25CE%25B4%25CF%258D%25CF%2583%25CE%25B7%2520%25CF%2584%25CE%25BF%25CF%2585%2520%25CE%25B7%25CE%25BB%25CE%25AF%25CE%25BF%25CF%2585/
↺ https://pexels.com/nb-no/sok/solnedgang/
↺ https://pexels.com/ro-ro/cauta/apus/
↓ /home/spypunk/output/pexels-photo-1237119.jpeg [1 KB]
↓ /home/spypunk/output/pexels-photo-457882.jpeg [1 KB]
↓ /home/spypunk/output/pexels-photo-355465.jpeg [1 KB]
↓ /home/spypunk/output/pexels-photo-66997.jpeg [10 KB]
↓ /home/spypunk/output/pexels-photo-912110.jpeg [1 KB]
↓ /home/spypunk/output/pexels-photo-414171.jpeg [1 KB]
↓ /home/spypunk/output/pexels-photo-551578.jpeg [23 KB]
↺ https://pexels.com/ru-ru/search/%25D0%25B7%25D0%25B0%25D0%25BA%25D0%25B0%25D1%2582/
↺ https://pexels.com/tr-tr/arama/g%25C3%25BCn%2520bat%25C4%25B1m%25C4%25B1/
↓ /home/spypunk/output/pexels-photo-561463.jpeg [20 KB]
↓ /home/spypunk/output/nastasia-789.jpeg [5 KB]
↺ https://pexels.com/sk-sk/vyhladat/z%25C3%25A1pad%2520slnka/
↓ /home/spypunk/output/skitterphoto-189.jpeg [5 KB]
...
~~~
## What about license?
This project is licensed under the WTFPL (Do What The Fuck You Want To Public License, Version 2)

[![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/logo-160x116.png)](http://www.wtfpl.net/)

Copyright © 2019 spypunk [spypunk@gmail.com](mailto:spypunk@gmail.com)

This work is free. You can redistribute it and/or modify it under the terms of the Do What The Fuck You Want To Public License, Version 2, as published by Sam Hocevar. See the COPYING file for more details.
