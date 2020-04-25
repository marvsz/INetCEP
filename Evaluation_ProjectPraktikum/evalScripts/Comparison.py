import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

Results = {'Name':[], 'Message_Overhead': [], 'SimTime': [], 'Complex Events': []}
qi= [10, 5, 1, 0.1] # (s)
sim_time=300 #(s)
meaningful_event= sim_time/300

for i in qi:
    Results['Name'].append(i)
    number_of_messages=sim_time/i
    Results['SimTime'].append(sim_time)
    Results['Complex Events'].append(meaningful_event)
    Results['Message_Overhead'].append(number_of_messages)

sim_time*=2
meaningful_event= sim_time/300
for i in qi:
    Results['Name'].append(i)
    number_of_messages=sim_time/i
    Results['SimTime'].append(sim_time)
    Results['Complex Events'].append(meaningful_event)
    Results['Message_Overhead'].append(number_of_messages)

for items in Results['Complex Events']:
    print items
#Results['Event'].remove(0)

df=pd.DataFrame(Results)
sns.set(style="whitegrid", font_scale=1.5)
ax = sns.lineplot(x="Name", y="Message_Overhead", hue = "Complex Events", data=df)
ax.set(xlabel='Polling Frequency (s)',  ylabel ='Number of query interests')

plt.show()
