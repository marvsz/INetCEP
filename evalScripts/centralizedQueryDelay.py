import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

Results = {'Name':[], 'Value':[], 'GC': [], 'Place': [], 'Comm': []}
val1 = []
val2 = []
val3 = []
val4 = []
val5 = []
val6 = []
val7 = []
val8 = []
with open("run2/q1") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("Q1")
            Results['Value'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            Results['GC'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['Place'].append(float(splitLine[4].replace('\n', ' ').replace('\r', '')))
            Results['Comm'].append(float(splitLine[5].replace('\n', ' ').replace('\r', '')))
   
with open("run2/q2") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("Q2")
            Results['Value'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            Results['GC'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['Place'].append(float(splitLine[4].replace('\n', ' ').replace('\r', '')))
            Results['Comm'].append(float(splitLine[5].replace('\n', ' ').replace('\r', '')))
            
with open("run2/q3") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("Q3")
            Results['Value'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            Results['GC'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['Place'].append(float(splitLine[4].replace('\n', ' ').replace('\r', '')))
            Results['Comm'].append(float(splitLine[5].replace('\n', ' ').replace('\r', '')))
            
with open("run2/q4") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("Q4")
            Results['Value'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            Results['GC'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['Place'].append(float(splitLine[4].replace('\n', ' ').replace('\r', '')))
            Results['Comm'].append(float(splitLine[5].replace('\n', ' ').replace('\r', '')))
            
with open("run2/q5") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("Q5")
            Results['Value'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            Results['GC'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['Place'].append(float(splitLine[4].replace('\n', ' ').replace('\r', '')))
            Results['Comm'].append(float(splitLine[5].replace('\n', ' ').replace('\r', '')))
            
with open("run2/q6") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("Q6")
            Results['Value'].append(float(splitLine[2].replace('\n', ' ').replace('\r', '')))
            Results['GC'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['Place'].append(float(splitLine[4].replace('\n', ' ').replace('\r', '')))
            Results['Comm'].append(float(splitLine[5].replace('\n', ' ').replace('\r', '')))

#Results['Name'].append("Q1")
#Results['Value'].append(val1)
#Results['Name'].append("Q2")
#Results['Value'].append(val2)
#Results['Name'].append("Q3")
#Results['Value'].append(val3)
#Results['Name'].append("Q4")
#Results['Value'].append(val4)
#Results['Name'].append("Q5")
#Results['Value'].append(val5)
#Results['Name'].append("Q6")
#Results['Value'].append(val6)
#Results['Name'].append("Q7")
#Results['Value'].append(val7)
#Results['Name'].append("Q8")
#Results['Value'].append(val8)
df = pd.DataFrame(Results)

m0 = df.groupby(['Name'])['Value'].median().values
print(m0)

m1 = df.groupby(['Name'])['GC'].median().values
print(m1)

m2 = df.groupby(['Name'])['Place'].median().values
print(m2)

m3 = df.groupby(['Name'])['Comm'].median().values
print(m3)

#options(tibble.print_max = 1e9)
#with pd.option_context('display.max_rows', None, 'display.max_columns', None):
    #print(df)
#tips = sns.load_dataset("tips")
#print(tips)
sns.set(style="whitegrid", font_scale=1.5)
ax = sns.boxplot(x="Name", y="Comm", data = df, palette=["C0", "C1", "C2", "C3", "C4", "C5"])
ax.set(xlabel='Query', ylabel ='Communication Delay (ms)')
#ax = sns.boxplot(x="Name", y="Value", data = df, palette=["C0", "C1", "C2", "C3", "C4", "C5"])
#ax.set(xlabel='Query', ylabel ='Total Delay (ms)')
#print(df)
plt.show()
