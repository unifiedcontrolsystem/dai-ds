import argparse
import os
from sqlalchemy import create_engine, DDL


def connect_to_db(db_url):
    """Establish connection to DB
    :param db_url: The database connection string in the format
    postgresql[+<driver>://[<username>[:<password>]]@<server>[:<port>]/<database>.d
    :return:  Database engine
    """
    return create_engine(db_url)


def enable_partition_and_purge_trigger(db_engine, table_name, column_name, purge_data):
    """
    :param db_engine:  The database engine to execute the SQL statements
    :param table_name: The name of the table to apply the partition
    :param column_name: The column name to partition on.  This column need to
    be a Postgres timestamp data type
    :return:  None.  Only print output.
    """

    generated_ddl_trigger_stmt = ("SELECT generate_partition_purge_rules('%s', '%s', '%s');""" %
                                      (table_name, column_name, purge_data))
    try:
        partition_trigger_ddl_stmt = db_engine.execute(
        generated_ddl_trigger_stmt).scalar()
    except Exception:
        raise Exception
    db_engine.execute(DDL(partition_trigger_ddl_stmt))

    # Verify the purge trigger and the partition handler functions do exist
    if purge_data > 0:
        verify_trigger_stmt = ("SELECT 1 "
                           "FROM information_schema.triggers "
                           "WHERE trigger_name = 'purge_%s_trigger';" %
                           table_name)
        verify_trigger_result = db_engine.execute(verify_trigger_stmt).scalar()
        if verify_trigger_result:
            print("Verified 'purge_%s_trigger" % table_name)
        else:
            raise RuntimeError("The expected trigger 'purge_%s_trigger' does not "
                           "exist after enabled purging on %s" %
                           (table_name, table_name))

def disable_partition_and_purge_trigger(db_engine, table_name):
    """
    :param db_engine:  The database engine to execute the SQL statements
    :param table_name: The name of the table to construct the name of the
    insert trigger and partition handler.
    :return:  None.  Only print output.
    """
    drop_trigger_ddl_stmt = (
        "DROP RULE IF EXISTS autocall_createfuturepartitions_%s ON %s RESTRICT; "
        "DROP TRIGGER IF EXISTS purge_%s_trigger ON %s RESTRICT;" %
        (table_name, table_name, table_name, table_name))
    db_engine.execute(DDL(drop_trigger_ddl_stmt))
    drop_function_ddl_stmt = (
        "DROP FUNCTION IF EXISTS createfuturepartitions_%s(timestamp without time zone) RESTRICT;"
        "DROP FUNCTION IF EXISTS purge_%s() RESTRICT;" % table_name, table_name)
    db_engine.execute(DDL(drop_function_ddl_stmt))

    print(
        "Purge trigger and partition rule disabled for table '%s.'  Existing partitions left "
        "as is." % table_name)


def main():
    main_parser = argparse.ArgumentParser(add_help=False,
        description="PostgreSQL Table Partition Helper.  Use this program to "
                    "enable table partitioning with and without auto dropping "
                    "of old partitions.  This program can also be used to "
                    "disable table partition.")
    # The parent_parser is to parse the common arguments
    parent_parser = argparse.ArgumentParser(add_help=False)
    parent_parser.add_argument("-d", "--db_url", type=str, default="",
        metavar="postgresql+driver://username:password@host:port/database",
        help="The database connection string in the format "
             "expected by SQLAlchemy.  The default value can "
             "be set in environment variable "
             "'PG_DB_URL'")
    parent_parser.add_argument("table", type=str,
        help="The name of the table to enable or disable partitioning.")

    subparsers = main_parser.add_subparsers(dest='subparser_name',
        help="Enable or Disable partition subcommands")
    enable_parser = subparsers.add_parser("enable", parents=[parent_parser],
                                          help="Enable partition")
    disable_parser = subparsers.add_parser("disable", parents=[parent_parser],
                                           help="Disable partition")

    enable_parser.add_argument("column", type=str,
        help="The name of the column to partition on.  This column needs to be "
             "a PostgreSQL timestamp data type.")
    enable_parser.add_argument("--purge", type=int, default=6,
        help="Enable purging of this table and keep only 6 months worth of data. Default value is false.")

    args = main_parser.parse_args()
    if args.db_url:
        db_url = args.db_url
    else:
        db_url = os.getenv("PG_DB_URL")
    if not db_url:
        raise RuntimeError("Neither --db_url option is specified nor The "
                           "'PG_DB_URL' environment variable is set.  Please "
                           "use the --db_url command-line option or set the "
                           "environment variable with the following pattern:\n"
                           "postgresql[+<driver>://[<username>[:<password>]]"
                           "@<server>[:<port>]/<database>\n\n"
                           "http://docs.sqlalchemy.org/en/latest/core/"
                           "engines.html#database-urls")

    db_engine = connect_to_db(db_url=db_url)
    if args.subparser_name == "enable":
        enable_partition_and_purge_trigger(db_engine, args.table, args.column,
                                 args.purge)
    if args.subparser_name == "disable":
        disable_partition_and_purge_trigger(db_engine, args.table)


if __name__ == "__main__":
    main()
