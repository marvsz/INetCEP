import pandas as pd
import seaborn as sns
import numpy as np
import matplotlib.pyplot as plt

Results = {'Name':[], 'Value':[], 'PathBDP': [], 'PathEnergy': []}
val1 = []
val2 = []
val3 = []
val4 = []
val5 = []
val6 = []
val7 = []
val8 = []
   
with open("run3/output_B") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("10")
            Results['Value'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['PathEnergy'].append(float(splitLine[9].replace('\n', ' ').replace('\r', '')))
            Results['PathBDP'].append(float(splitLine[10].replace('\n', ' ').replace('\r', '')))
           
            
with open("run3/output_C") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("20")
            Results['Value'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['PathEnergy'].append(float(splitLine[9].replace('\n', ' ').replace('\r', '')))
            Results['PathBDP'].append(float(splitLine[10].replace('\n', ' ').replace('\r', '')))
           
            
with open("run3/output_D") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("30")
            Results['Value'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['PathEnergy'].append(float(splitLine[9].replace('\n', ' ').replace('\r', '')))
            Results['PathBDP'].append(float(splitLine[10].replace('\n', ' ').replace('\r', '')))
                       
with open("run3/output_A") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("40")
            Results['Value'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['PathEnergy'].append(float(splitLine[9].replace('\n', ' ').replace('\r', '')))
            Results['PathBDP'].append(float(splitLine[10].replace('\n', ' ').replace('\r', '')))
            
with open("run3/output_G") as f:
    for line in f:
        if line.strip():
            splitLine=line.split(",")
            Results['Name'].append("50")
            Results['Value'].append(float(splitLine[3].replace('\n', ' ').replace('\r', '')))
            Results['PathEnergy'].append(float(splitLine[9].replace('\n', ' ').replace('\r', '')))
            Results['PathBDP'].append(float(splitLine[10].replace('\n', ' ').replace('\r', '')))
            
df = pd.DataFrame(Results)
#options(tibble.print_max = 1e9)
#with pd.option_context('display.max_rows', None, 'display.max_columns', None):
    #print(df)
#tips = sns.load_dataset("tips")
#print(tips)
sns.set(style="whitegrid", font_scale=1.5)
ax = sns.boxplot(x="Name", y="Value", data = df, palette=["C2"], showmeans=True)
ax.set(xlabel='Queries', ylabel ='Total Delay in ms')
m1 = df.groupby(['Name'])['Value'].median().values
print(m1)
mL1 = [str(np.round(s, 2)) for s in m1]

'''ind = 0
for tick in range(len(ax.get_xticklabels())):
    ax.text(tick-.2, m1[ind+1]+1, mL1[ind+1],  horizontalalignment='center',  color='w', weight='semibold')
    ax.text(tick+.2, m1[ind]+1, mL1[ind], horizontalalignment='center', color='w', weight='semibold')
    ind += 2 
#print(df)'''
plt.show()
