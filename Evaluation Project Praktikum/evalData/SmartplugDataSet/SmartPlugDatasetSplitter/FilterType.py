import time
start = time.time()
mystring = ""
file = open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/filtered.txt", "w")
file.close()
myList = list(mystring)
limiter = 0
millionCounter = 0
with open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/sorted.csv") as f:
    for line in f:
        if(line.split(",")[3] == "1"):
            myList.append(line)
            limiter = limiter + 1
            if(limiter >= 10000000):
                file = open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/filtered.txt", "a")
                file.write("".join(myList))
                file.close()
                myList.clear()
                limiter = 0
                millionCounter = millionCounter + 1
                end = time.time()
                print("Added another 10 million, now " + str(millionCounter) + "0 million lines have been added. "+ str(round((end - start)/60)) + " Minutes elapsed")
    file = open("/media/johannes/Bulk/DEBS Grand Challenge Dataset/filtered.txt", "a")
    file.write("".join(myList))
    file.close()
    myList.clear()
end = time.time()
print("Done initial processing. Took " + str(round((end - start)/60)) + " minutes.")
