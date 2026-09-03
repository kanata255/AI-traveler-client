CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `age` INT,
    `email` VARCHAR(100),
    PRIMARY KEY (`id`)
);

INSERT INTO `user` (name, age, email) VALUES ('Jack', 20, 'jack@baomidou.com');
INSERT INTO `user` (name, age, email) VALUES ('Jone', 21, 'jone@baomidou.com');
INSERT INTO `user` (name, age, email) VALUES ('Timmy', 22, 'timmy@baomidou.com');
INSERT INTO `user` (name, age, email) VALUES ('Alice', 23, 'alice@baomidou.com');
INSERT INTO `user` (name, age, email) VALUES ('Bob', 24, 'bob@baomidou.com');
