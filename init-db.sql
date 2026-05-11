CREATE DATABASE IF NOT EXISTS auth_service_db;
CREATE DATABASE IF NOT EXISTS room_db;
CREATE DATABASE IF NOT EXISTS reservation_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS staff_db;

CREATE USER IF NOT EXISTS 'hotel_user'@'%' IDENTIFIED BY 'Hotel_1234';

GRANT ALL PRIVILEGES ON auth_service_db.* TO 'hotel_user'@'%';
GRANT ALL PRIVILEGES ON room_db.* TO 'hotel_user'@'%';
GRANT ALL PRIVILEGES ON reservation_db.* TO 'hotel_user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'hotel_user'@'%';
GRANT ALL PRIVILEGES ON staff_db.* TO 'hotel_user'@'%';

FLUSH PRIVILEGES;
