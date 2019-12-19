"""DAI manager schema changes and increasing the column sizes

Revision ID: d5eeb6abefb3
Revises: 9e2f2d91b5a6
Create Date: 2019-01-28 16:21:08.314922

"""

# revision identifiers, used by Alembic.
revision = 'd5eeb6abefb3'
down_revision = '9e2f2d91b5a6'
branch_labels = None
depends_on = None

from alembic import op
import sqlalchemy as sa
import textwrap


def upgrade():
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_aggregatedenvdata ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_computenode_history ADD COLUMN aggregator CHARACTER VARYING(63) NOT NULL,
                                      ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_diag ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_servicenode_history ADD COLUMN sequencenumber int NOT NULL,
                                  ADD COLUMN aggregator CHARACTER VARYING(63) NOT NULL,
                                  ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_adapter_history ADD COLUMN lctn CHARACTER VARYING (25) NOT NULL,
                                  ADD COLUMN pid bigint NOT NULL, 
                                  ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_alert ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_bootimage_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_chassis_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_fabrictopology_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_job_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_jobstep_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_lustre_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_machine_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_rack_history ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_rasevent ADD COLUMN entrynumber bigserial;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_rasmetadata ADD COLUMN entrynumber bigserial NOT NULL;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_replacement_history ADD COLUMN entrynumber bigserial NOT NULL;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_serviceoperation_history ADD COLUMN entrynumber bigserial NOT NULL;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_switch_history ADD COLUMN entrynumber bigserial NOT NULL;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_wlmreservation_history ADD COLUMN entrynumber bigserial NOT NULL;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_workitem_history ADD COLUMN entrynumber bigserial NOT NULL;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_servicenode_history ALTER COLUMN inventoryinfo TYPE CHARACTER VARYING (16384);"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_inventorysnapshot ALTER COLUMN inventoryinfo TYPE CHARACTER VARYING (16384);"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_computenode_history ALTER COLUMN inventoryinfo TYPE CHARACTER VARYING (16384);"""))
    op.execute(textwrap.dedent(""" ALTER TYPE inventorytype ALTER ATTRIBUTE inventoryinfo set data type character varying (16384);"""))



def downgrade():
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_servicenode_history ALTER COLUMN inventoryinfo TYPE CHARACTER VARYING (1000);"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_inventorysnapshot ALTER COLUMN inventoryinfo TYPE CHARACTER VARYING (1000);"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_computenode_history ALTER COLUMN inventoryinfo TYPE CHARACTER VARYING (1000);"""))
    op.execute(textwrap.dedent(""" ALTER TYPE inventorytype ALTER ATTRIBUTE inventoryinfo set data type character varying (1000);"""))

    op.execute(textwrap.dedent(""" ALTER TABLE tier2_workitem_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_wlmreservation_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_switch_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_serviceoperation_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_replacement_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_rasmetadata DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_rasevent DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_rack_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_machine_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_lustre_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_jobstep_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_job_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_fabrictopology_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_chassis_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_bootimage_history DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_alert DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_adapter_history DROP COLUMN lctn,
                                  DROP COLUMN pid, 
                                  DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_servicenode_history DROP COLUMN sequencenumber,
                                  DROP COLUMN aggregator,
                                  DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_diag DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_computenode_history DROP COLUMN aggregator,
                                      DROP COLUMN entrynumber;"""))
    op.execute(textwrap.dedent(""" ALTER TABLE tier2_aggregatedenvdata DROP COLUMN entrynumber;"""))
