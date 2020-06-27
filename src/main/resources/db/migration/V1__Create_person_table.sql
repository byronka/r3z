CREATE TABLE TIMEANDEXPENSES.PERSON (
    id serial PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE TIMEANDEXPENSES.PROJECT (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE TIMEANDEXPENSES.TIMEENTRY (
    id serial PRIMARY KEY,
    user INTEGER NOT NULL,
    project INTEGER NOT NULL,
    time_in_minutes INTEGER NOT NULL,
    date DATE NOT NULL,
    -- See Details.kt for details about the max length
    details VARCHAR(500),
    FOREIGN KEY (project) REFERENCES PROJECT (id)
);
