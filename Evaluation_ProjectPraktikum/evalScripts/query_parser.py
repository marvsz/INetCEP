import matplotlib.pyplot as plt
#import numpy as np
import pandas as pd
import seaborn as sns

Results = {'Nested Queries':[], 'Number': [], 'Time':[]}

with open("parser_filter.txt") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Nested Queries'].append("Query 2")
            Results['Time'].append(float(splitLine[1].replace('\n', ' ').replace('\r', '')))
            Results['Number'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            
with open("parser_join.txt") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Nested Queries'].append("Query 3")
            Results['Time'].append(float(splitLine[1].replace('\n', ' ').replace('\r', '')))
            Results['Number'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))

df=pd.DataFrame(Results)
sns.set(style="whitegrid", font_scale=1.5)
ax = sns.lineplot(x="Number", y="Time", hue="Nested Queries", palette=["C0", "C1"], data=df)
ax.set(xlabel='Number of Operators',  ylabel ='Runtime (ms)')
plt.show()
