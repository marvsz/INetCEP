from collections import defaultdict
import time
import os

def get_immediate_subdirectories(a_dir):
    return [name for name in os.listdir(a_dir)
            if os.path.isdir(os.path.join(a_dir, name))]

start = time.time()
millionCounter = 0
limiter = 0
housePath = "/media/johannes/Bulk/DEBS Grand Challenge Dataset/houses/"

for subdir in get_immediate_subdirectories(housePath):
    print("started processing "+subdir)
    subdirectoryPath = housePath + subdir + "/"
    for filename in os.listdir(subdirectoryPath):
        if (filename[-4:] == ".txt"):
            with open(subdirectoryPath + filename) as f:
                householdDict = defaultdict(list)
                folder = filename[:-4] + "/"
                houseHoldPath = subdirectoryPath + folder
                if not os.path.exists(houseHoldPath):
                    os.makedirs(houseHoldPath)
                for line in f:
                    limiter = limiter + 1
                    householdDict[int(line.split(",")[4])].append(line)
                    if limiter >= 10000000:
                        millionCounter = millionCounter + 1
                        for k in householdDict.keys():
                            newFilename = "plug" + str(k)
                            file = open(houseHoldPath + newFilename + ".txt", "a")
                            file.write("".join(householdDict[k]))
                            file.close()
                            householdDict[k].clear()
                            limiter = 0
                        end = time.time()
                        print("Processed another 10 million lines. " + str(
                            millionCounter) + "0 million lines have been processed." + str(
                            round((end - start) / 60)) + " Minutes elapsed")
                for k in householdDict.keys():
                    newFilename = "plug" + str(k)
                    file = open(houseHoldPath + newFilename + ".txt", "a")
                    file.write("".join(householdDict[k]))
                    file.close()
                    householdDict[k].clear()
    print("stopped processing " + subdir)

end = time.time()
print("Done Processing the plugs. Took " + str(round((end - start) / 60)) + " Minutes")