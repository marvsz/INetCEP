from collections import defaultdict
import time

start = time.time()
houseDict = defaultdict(list)
millionCounter = 0
limiter = 0
with open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/filtered.txt") as f:
    for line in f:
        limiter = limiter + 1
        houseDict[int(line.split(",")[6])].append(line)
        if(limiter >= 10000000):
            millionCounter = millionCounter + 1
            for k in houseDict.keys():
                filename = "house" + str(k)
                file = open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/houses/"+filename+".txt", "a")
                file.write("".join(houseDict[k]))
                file.close()
                houseDict[k].clear()
                limiter = 0
            end = time.time()
            print("Processed another 10 million lines. " + str(millionCounter) + "0 million lines have been processed. " + str(round((end - start)/60)) + " Minutes elapsed")
    for k in houseDict.keys():
        filename = "house" + str(k)
        file = open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/houses/" + filename + ".txt", "a")
        file.write("".join(houseDict[k]))
        file.close()
        houseDict[k].clear()
end = time.time()
print("Done Processing the housees. Took "+str(round((end-start)/60)) + " Minutes.")