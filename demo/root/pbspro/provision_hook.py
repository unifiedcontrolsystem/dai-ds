# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
import pbs
import sys
import os
import time

pbs_event = pbs.event()
vnode = pbs_event.vnode
aoe   = pbs_event.aoe


class BootImageInfo:
	def __init__(self, rc=-99999, bootImage="", bootStrap=""):
		self.rc        = rc
		self.bootImage = bootImage
		self.bootStrap = bootStrap
	def getRc(self):
		return self.rc
	def getBootImage(self):
		return self.bootImage
	def getBootStrap(self):
		return self.bootStrap


def doesProvBootImagesMatchDataStoreImages(vnode, aoe, bRefetchDsInfo):
	################################################################################
	# Ensure that the data store has the boot image information.
	################################################################################
	# Check & see that file of boot image info already exists, if it doesn't fill it in.
	fileBootImages = "/tmp/provision_hook-BootImages"
	if (bRefetchDsInfo == True) or (not os.path.isfile(fileBootImages)):
		cmdGetBootImages = "echo 'SELECT Id, State, BootImageFile, BootImageChecksum, BootStrapImageFile, BootStrapImageChecksum, LastChgTimestamp FROM BootImage ORDER BY Id;' | /opt/voltdb/bin/sqlcmd > " + fileBootImages
		#print cmdGetBootImages
		os.system(cmdGetBootImages)
	# Open the file using with to ensure that the file is closed automatically (even if an exception is raised)
	bGotDsBootInfo = False  # Flag indicating if we got the data store's boot image info.
	sBootImage = ""
	checksumBootImage = -1
	sBootStrap = ""
	checksumBootStrap = -1
	bStartOfImages = False
	with open(fileBootImages, 'r') as fBootImages:
		for line in fBootImages:
			line = line.strip()
			words = line.split()
			if len(line) > 0:
				if line.find("--") >= 0:
					bStartOfImages = True
				else:
					if words[0] == "(Returned":
						bStartOfImages = False
					if (bStartOfImages == True) and (words[0] == aoe):
						bGotDsBootInfo = True
						sBootImage = words[2]
						checksumBootImage = words[3]
						sBootStrap = words[4]
						checksumBootStrap = words[5]
	if bGotDsBootInfo == False:
		#print "%%%%%%%%%%%  Could not find the specified AOE in the data store's boot images  %%%%%%%%%%%%%%%%%%%%%%%%%"
		return BootImageInfo(-4)

	################################################################################
	# Ensure that the checksum we have matches the Provisioner's checksums for these bootimages.
	################################################################################
	# Run commands to get the checksum values from Warewulf into a file.
	fileProvChecksums = "/tmp/provision_hook-" + vnode
	cmdProvBootImageChecksum = "wwsh object print -p checksum " + sBootImage + " > "  + fileProvChecksums + " 2>&1"
	cmdProvBootStrapChecksum = "wwsh object print -p checksum " + sBootStrap + " >> " + fileProvChecksums + " 2>&1"
	#print "cmdProvBootImageChecksum =", cmdProvBootImageChecksum
	os.system(cmdProvBootImageChecksum)
	#print "cmdProvBootStrapChecksum =", cmdProvBootStrapChecksum
	os.system(cmdProvBootStrapChecksum)
	# Get the ww checksum values out of the file.
	checksumProvBootImage = -2
	checksumProvBootStrap = -2
	with open(fileProvChecksums, 'r') as fProvChecksums:
		lineCntr = 0
		for line in fProvChecksums:
			line = line.strip()
			if len(line) > 0:
				if line.find("No matching objects found") >= 0:
					if lineCntr == 0:
						#print "%%%%%%%%%%%  Boot Image was not found in Warewulf  %%%%%%%%%%%%%%%%%%%%%%%%%"
						return BootImageInfo(-2, sBootImage, sBootStrap)
					else:
						#print "%%%%%%%%%%%  Bootstrap Image was not found in Warewulf  %%%%%%%%%%%%%%%%%%%%%%%%%"
						return BootImageInfo(-3, sBootImage, sBootStrap)
				else:
					if lineCntr == 0:
						checksumProvBootImage = line
					else:
						checksumProvBootStrap = line
			lineCntr = lineCntr + 1
	# Compare the Warewulf checksums against the expected checksums (from the data store).
	if (checksumBootImage == checksumProvBootImage) and (checksumBootStrap == checksumProvBootStrap):
		#print "%%  checksums match  %%"
		return BootImageInfo(0, sBootImage, sBootStrap)
	elif (checksumBootImage != checksumProvBootImage):
		#print "%%  ERROR - Boot image checksum does not match the expected checksum!  %%"
		return BootImageInfo(-5)
	elif (checksumBootStrap != checksumProvBootStrap):
		#print "%%  ERROR - Boot strap checksum does not match the expected checksum!  %%"
		return BootImageInfo(-6)
	else:
		return BootImageInfo(-1)
#  End doesProvBootImagesMatchDataStoreImages(vnode, aoe, bRefetchDsInfo)


################################################################################
# Check & make sure that the specified boot images are available in both data store and warewulf.
################################################################################
bRefetchDsInfo = False  # Don't need to refetch boot image data from data store.
bootImageInfo = doesProvBootImagesMatchDataStoreImages(vnode, aoe, bRefetchDsInfo);
if bootImageInfo.getRc() < 0:
	# Did not get a match on the data store images - try re-fetching the boot image data from data store and checking again.
	bRefetchDsInfo = True # Force a re-fetch of the information from the data store
	pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - initial check of the boot images did not produce a match to the expected values, re-fetching the info from the data store and will then double check!")
	#print "%%  Initial check of the boot images did not produce a match to the expected values, re-fetching the info from the data store and will then double check!  %%"
	bootImageInfo = doesProvBootImagesMatchDataStoreImages(vnode, aoe, bRefetchDsInfo);

# Check & see if the provisioner's checksums matched the expected checksums from the data store.
if bootImageInfo.getRc() == 0:
	pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - provisioner checksums match the expected checksums from the data store")
	#print "%%  Provisioner checksums match the expected checksums from the data store  %%"
else:
	if bootImageInfo.getRc() == -1:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Boot image checksums do not match the expected checksums!")
		#print "%%  ERROR - Boot image checksums do not match the expected checksums!  %%"
		pbs_event.reject("Boot image checksums do not match the expected checksums!", 11)
	elif bootImageInfo.getRc() == -2:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Boot Image, " + bootImageInfo.getBootImage() + ", was not found in Warewulf!")
		#print "%%%%%%%%%%%  Boot Image was not found in Warewulf  %%%%%%%%%%%%%%%%%%%%%%%%%"
		pbs_event.reject("Boot Image was not found in Warewulf!", 12)
	elif bootImageInfo.getRc() == -3:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Bootstrap, " + bootImageInfo.getBootStrap() + ", was not found in Warewulf!")
		#print "%%%%%%%%%%%  Bootstrap Image was not found in Warewulf  %%%%%%%%%%%%%%%%%%%%%%%%%"
		pbs_event.reject("Bootstrap Image was not found in Warewulf!", 13)
	elif bootImageInfo.getRc() == -4:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - could not find the specified AOE, " + aoe + ", in the data store's list of boot images!")
		#print "%%%%%%%%%%%  Could not find the specified AOE in the data store's list of boot images!  %%%%%%%%%%%%%%%%%%%%%%%%%"
		pbs_event.reject("Could not find the specified AOE in the data store's list of boot images!", 14)
	elif bootImageInfo.getRc() == -5:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Boot image checksum does not match the expected checksum! ")
		#print "%%%%%%%%%%%  Boot image checksum does not match the expected checksum!   %%%%%%%%%%%%%%%%%%%%%%%%%"
		pbs_event.reject("Boot image checksum does not match the expected checksum!", 15)
	elif bootImageInfo.getRc() == -6:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Boot strap checksum does not match the expected checksum! ")
		#print "%%%%%%%%%%%  Boot strap checksum does not match the expected checksum!   %%%%%%%%%%%%%%%%%%%%%%%%%"
		pbs_event.reject("Boot strap checksum does not match the expected checksum!", 16)
	else:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - doesProvBootImagesMatchDataStoreImages() returned " + rc + " !")
		#print "%%%%%%%%%%%  doesProvBootImagesMatchDataStoreImages() returned an unexpected rc!  %%%%%%%%%%%%%%%%%%%%%%%%%"
		pbs_event.reject("doesProvBootImagesMatchDataStoreImages() returned an unexpected rc!", 17)


################################################################################
# Shutdown the node
################################################################################
#pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - /usr/bin/ssh -o StrictHostKeyChecking=no " + vnode + " service pbs stop > /dev/null 2>&1")
#ret = os.system("/usr/bin/ssh -o StrictHostKeyChecking=no " + vnode + " service pbs stop > /dev/null 2>&1")
#if ret != 0:
#	pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Failed to run the command to shutdown pbs on the node - FAIL!")
#	pbs_event.reject("Provisioning failed!", 10)
pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - wwsh ipmi powercycle " + vnode + " > /dev/null 2>&1")
ret = os.system("wwsh ipmi powercycle " + vnode + " > /dev/null 2>&1")


#################################################################################
## Wait 10 seconds before looking to ensure that the node is down.
#################################################################################
#pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - sleep 10 seconds")
#time.sleep(10) # delays for 10 sec


################################################################################
# Ping the machine waiting until it is not responsive before continuing.
################################################################################
pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - Pinging machine until it is gone down...")
timeout = 120
ticks = 0
bMachShutdown = 0
while bMachShutdown != 1:
	ret = os.system("ping -c 1 -i 5 -w 10 " + vnode + " > /dev/null 2>&1")
	if not ret:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - the machine " + vnode + " is still up, ticks=%d" % (ticks))
		ticks = ticks + 1
		if ticks > timeout:
			pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - The machine " + vnode + " didn't power off even after 2 minutes - FAIL!")
			pbs_event.reject("Provisioning failed!", 30)
		else:
			time.sleep(1) # delays for 1 sec
	else:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - The machine " + vnode + " is now shutdown")
		bMachShutdown = 1


#################################################################################
## Wait 2 seconds
#################################################################################
pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - sleep 2 seconds")
time.sleep(2) # delays for 2 sec


################################################################################
# Change the WW config so that the new image will be used when this node comes back up.
################################################################################
fileProvChgImages = "/tmp/provision_hook-" + vnode
cmdProvChgImages = "wwsh -y provision set " + vnode + " --vnfs=" + bootImageInfo.getBootImage() + " --bootstrap=" + bootImageInfo.getBootStrap() + " > " + fileProvChgImages + " 2>&1"
pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - " + cmdProvChgImages)
rc = os.system(cmdProvChgImages)
with open(fileProvChgImages, 'r') as fProvChgImages:
	for line in fProvChgImages:
		line = line.strip()
		if (len(line) > 0) and (line.find("ERROR") >= 0):
			pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Failure during change of warewulf configuration!")
			#print "%%  ERROR - Failure during change of warewulf configuration!  %%"
			pbs_event.reject("Failure during change of warewulf configuration!", 20)
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ## - line=" + line)


################################################################################
# Power On the node (it will boot up with the new image).
################################################################################
fileProvPowerOn = "/tmp/provision_hook-" + vnode
cmdProvPowerOn  = "wwsh ipmi poweron " + vnode + " >> " + fileProvPowerOn + " 2>&1"
delay = 1  # set initial delay value to 1 second between retries
bNodePoweredBackOn = False;
cntrTry = 1
while (bNodePoweredBackOn == False) and (cntrTry <= 10):
	time.sleep(delay)
	pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - " + cmdProvPowerOn + ", try=%d" % (cntrTry))
	rc = os.system(cmdProvPowerOn)
	with open(fileProvPowerOn, 'r') as fProvProvPowerOn:
		for line in fProvProvPowerOn:
			line = line.strip()
			# See if there are any characters in this line
			if (len(line) > 0):
				if ((line.find("Domain") >= 0) and (line.find("started") >= 0)) or (line.find("Domain is already active") >= 0):
					pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ww did power this node back on, try=%d" % (cntrTry))
					bNodePoweredBackOn = True
				elif (line.find("Failed to start domain")):
					pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - #### - line=" + line + ", try=%d" % (cntrTry))
					if (delay == 1):
						pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - #### - ww failed to start up the node, changing to wait 30 secs between retries!")
						delay = 30  # change the delay to 30 seconds between retries.
				else:
					pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - #### - line=" + line + ", try=%d" % (cntrTry))
	cntrTry += 1
if bNodePoweredBackOn == False:
	pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - Unable to get Warewulf to restart the node - FAIL!")
	pbs_event.reject("Provisioning failed!", 45)


#################################################################################
## Wait 45 seconds before looking to ensure that the node is back up.
#################################################################################
#pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - sleep 45 seconds")
#time.sleep(45) # delays for 45 sec


################################################################################
# Ping the machine waiting until it is responsive before continuing.
################################################################################
pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - Pinging machine until it is back up...")
timeout = 120
ticks = 0
while 1:
	ret = os.system("ping -c 1 -i 5 -w 10 " + vnode + " > /dev/null 2>&1")
	if not ret:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - The machine " + vnode + " is now back up")
		pbs_event.accept(0)

	pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - the machine is not yet back up, ticks=%d" % (ticks))
	ticks = ticks + 1
	if ticks > timeout:
		pbs.logmsg(pbs.LOG_DEBUG, "provision_hook - " + vnode + " - ERROR - The machine " + vnode + " didn't come back up even after 2 minutes - FAIL!")
		pbs_event.reject("Provisioning failed!", 50)

