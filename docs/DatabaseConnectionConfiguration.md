# Configuration Guide for Online & Nearline Database Connections

## 1. Introduction
This document details the configuration options of the Online and Nearline Database connections for DAI/DS.

## 2. Online Tier Configuration
The Online Tier database connection is configured using _volt.ip_, located by default in _/opt/ucs/etc_.

###Example

```
127.0.0.1
```
The IP address is stored in plaintext, the above example points to a Volt DB that is located on the same
node as all of the Adapters.

## 3. Nearline Tier Database Configuration

The Nearline Tier database is configured using _NearlineConfig.json_, located by default in _/opt/ucs/etc_.


### Example

```json
{
    "db": {
        "type": "jdbc",
        "url": "jdbc:postgresql://localhost:5432/dai",
        "username": "dai",
        "password": "123@dai"
    }
}
```

### Reference

#### type
The API used to connect to the database. JDBC is the only currently implemented API.

#### url
URL at which the Nearline DB is located.

#### username
Username for Nearline Database

#### password
Password for Nearline Database