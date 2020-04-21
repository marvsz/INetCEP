from collections import defaultdict
import time
import os

start = time.time()

millionCounter = 0
limiter = 0
for filename in os.listdir("/media/johannes/Bulk/DEBS Grand Challenge Dataset/houses/"):
    if(filename[-4:]==".txt"):
        with open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/houses/"+filename) as f:
            householdDict = defaultdict(list)
            folder = filename[:-4] + "/"
            housePath = "/media/johannes/Bulk/DEBS Grand Challenge Dataset/houses/" + folder
            if not os.path.exists(housePath):
                os.makedirs(housePath)
            for line in f:
                limiter = limiter + 1
                householdDict[int(line.split(",")[5])].append(line)
                if limiter >= 10000000:
                    millionCounter = millionCounter + 1
                    for k in householdDict.keys():
                        newFilename = "household" + str(k)
                        file = open(housePath + newFilename + ".txt", "a")
                        file.write("".join(householdDict[k]))
                        file.close()
                        householdDict[k].clear()
                        limiter = 0
                    end = time.time()
                    print("Processed another 10 million lines. " + str(millionCounter) + "0 million lines have been processed." + str(round((end - start) / 60)) + " Minutes elapsed")
            for k in householdDict.keys():
                newFilename = "household" + str(k)
                file = open(housePath + newFilename + ".txt", "a")
                file.write("".join(householdDict[k]))
                file.close()
                householdDict[k].clear()
end = time.time()
print("Done Processing the households. Took "+str(round((end-start)/60)) + " Minutes")