# This file contains a discription on how to run the Complexity Test for Prediction 1 and Prediction 2
Please Reference the Readme in `src/test/` on how to set up the system Environment.

In `src/test/scala/nfn/service/PredictionComplecityAnalysis` is the Test for the Complexity of Prediction 1 and Prediction 2.

There are two ways to compare the two operators: First giving it a set of tuples and second giving it more different sources that conain tuples.

The Amount of tuples are set in line 17 in `val tuples`.
The Runtime Analysis for Prediction 1 happens in the methond `prediction1Analysis`. Here in line 78 in `val lines` are the files to consider.
If you want the prediction to run on more plugs to test for further complexity, you have to extend the join statement when giving it to the lines.

The Runtime Analysis for Prediction 2 happens in the mehtod `prediction2Analysis`.
Since prediction 2 is in theory capable of parallel operator processing we only measure the longest delay when processing a file.
TODO: 
- [ ] create joinFiles/joinStrings Method that takes a list and just joins everything.