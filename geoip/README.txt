Conversion from new MaxMind format to v.1 GeoIP.dat format courtesy of:
https://github.com/sherpya/geolite2legacy

Usage:

wget https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz
./geolite2legacy.py -i GeoLite2-Country-CSV.zip -f geoname2fips.csv -o GeoIP.dat

