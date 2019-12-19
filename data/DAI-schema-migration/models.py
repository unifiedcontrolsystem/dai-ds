#
# Copyright (c) 2019 Intel Corporation. All rights reserved
#
"""This module contains a set of classes that used by SQLAlchemy for
Object Relational Mapper (ORM)

"""

from distutils.version import StrictVersion
import os
import logging
import datetime
import alembic
from alembic.config import Config
from alembic import command
import sqlalchemy as sa
from sqlalchemy.orm import sessionmaker
from sqlalchemy import Column, create_engine
from sqlalchemy import DateTime
from sqlalchemy import ForeignKey, Integer, String
from sqlalchemy import Text, BOOLEAN
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext import declarative, compiler

# disabling Pylint error messages on SQLAlchemy classes and convention
# Invalid constant name.
# pylint: disable=C0103
# Too few public methods.
# pylint: disable=R0903
# Too many public methods.
# pylint: disable=R0904
# Class has no __init__ method.
# pylint: disable=W0232
# Redefining name '<name>' from outer scope.
# pylint: disable=W0621

# Minimum required version of SQLAlchemy and Alembic for the Database
# migration code to work correctly.  Specifically the naming convention feature
# that are necessary to provide predictable database constraints' names.
if StrictVersion(alembic.__version__) < StrictVersion("0.8.2"):
    raise RuntimeError("The Alembic version need to be at least 0.8.2.  "
                       "The installed version is %s" % alembic.__version__)
if StrictVersion(sa.__version__) < StrictVersion("0.9.2"):
    raise RuntimeError("The SQLAlchemy version need to be at least 0.9.2.  "
                       "The installed version is %s" % sa.__version__)


log = logging.getLogger(__name__)
log.setLevel(logging.DEBUG)


# Define a generic current timestamp function to be execute on the database
# server side
class CurrentTimeStamp(sa.sql.expression.FunctionElement):
    """Define a current timestamp function name that can be use to map to
    different database dialect implementations.
    """
    # Too many ancestors (12/7)
    # pylint: disable=R0901
    type = DateTime()


@compiler.compiles(CurrentTimeStamp, 'postgresql')
def postgres_current_timestamp(element, compiler, **kw):
    """Return the Postgres or MySQL function call to get the current DB
    server time

    :param element:  The element being construct
    :param compiler:  The compiler for postgresql
    :param kw:  Any additional argument
    :return: Postgres or MySQL function name for generating current timestamp
    """
    # Unused variable
    # pylint: disable=W0613
    return "CURRENT_TIMESTAMP"


Base = declarative.declarative_base()

# Naming convention for SQLAlchemy to auto generate the constrain name to
# have a predictable name.  Important for referencing constrains by name
# (e.g. alembic downgrade script).
Base.metadata.naming_convention = {
    "ix": 'ix_%(column_0_label)s',
    "uq": "uq_%(table_name)s_%(column_0_name)s",
    "ck": "ck_%(table_name)s_%(constraint_name)s",
    "fk": "fk_%(table_name)s_%(column_0_name)s_%(referred_table_name)s",
    "pk": "pk_%(table_name)s"}

metadata = Base.metadata

# Default session
session = sessionmaker()()

def setup(alembic_ini="schema-migration.ini"):
    """Setup all SQLAlchemy sessions and connections

    :param db_url: The database URL as required by SQLAlchemy
    :param alembic_ini: Alembic config file
    :return: None
    """
    alembic_conf = Config(alembic_ini)
    db_url = alembic_conf.get_section_option("alembic", "sqlalchemy.url")
    print(db_url)
    if not db_url:
        db_url = os.getenv("PG_DB_URL")
    if not db_url:
        raise RuntimeError("The db_url is not in the kwarg nor in the alembic_ini file.")
    sqlalchemy_db_url = sa.engine.url.make_url(db_url)

    print("-" * 50)
    msg = ("Connecting to '%s' database on %s server at '%s'" % (
        sqlalchemy_db_url.database,
        sqlalchemy_db_url.get_dialect().name.upper(),
        sqlalchemy_db_url.host))
    print(msg)
    engine = create_engine(db_url)
    Base.metadata.bind = engine
    # Base.metadata.bind.echo = True   # Log the SQL interaction.
    msg = (engine.execute("select 'Connected to %s database'" %
                          sqlalchemy_db_url.database).scalar())
    print(msg)
    msg = ("Established connection to '%s' database" %
           sqlalchemy_db_url.database)
    print(msg)

    print("-" * 50)
    print("Starting Alembic upgrade database to latest version")
    command.upgrade(alembic_conf, "head", tag=db_url)
    print("Completed running Alembic command")
    print("-" * 50)


if __name__ == "__main__":
    import pprint
    setup()
    dummy_query = session.query(Device).limit(10)
    print("Sample list of  Device")
    print(pprint.pformat(dummy_query.all()))

