package com.company;

import java.io.*;
import java.util.*;

/**
 * @author Ray Wang
 * @version 2/15/2020
 * <p>
 * This class defines the methods and constructs for a simple feed-forward neural network with any number of hidden layers, any
 * number of activations in each layer, and any number of output values. The network calculates activation values by taking a
 * weighted sum of all the activations in the layer before. The values of the weights can be inputted by the user in the attached
 * .txt file, and the class also contains a method that randomizes the weights between a certain inputted range. The values of
 * the activations in the first layer are also inputted by the user. The network can also calculate an error function defined as
 * E = (1/2) * (expectedValue - outputValue)^2. The network can minimize the error by taking the negative gradient of the error
 * function with respect to each individual weight. The network trains across every user-inputted test case by first randomizing
 * the weights, then taking the negative gradient of the error function with respect to each weight, multiplied by the learning
 * factor, for each test case until a certain error threshold is crossed or the maximum number iterations is reached. The network
 * utilizes the back propagation algorithm to apply changes to the weights on the fly in order to optimize the training process.
 * Documentation for the greek letter notation used to store values in the network(theta, omega, psi, lambda), as well as an
 * in-depth explanation on how back propagation works, can be found in the document "3-Minimizing and Optimizing the Error
 * Function."
 *
 * The network class contains the following instance variables:
 *    double[][]   activations: contains all the values of the activations
 *    double[][][] weights: contains all the values of the weights
 *    int[]        activationArraySizes: stores the number of nodes in each layer
 *    int          numLayers: stores the number of layers in the network
 *    double       learningFactor: stores the learning factor of the network
 *    double       minimumWeightValue: stores the minimum value a weight can be
 *    double       maximumWeightValue: stores the maximum value a weight can be
 *    double[][]   omegas: contains all the values of the omegas used to minimize the error (see: "3-Minimizing and Optimizing the
 *                 Error Function")
 *    double[][]   thetas: contains all the values of the thetas used to minimize the error (see: "3-Minimizing and Optimizing the
 *                 Error Function")
 *    double[][]   testCases: stores the values of the input activations for every test case
 *    int          numTestCases: stores the number of test cases
 *    double[]     expectedOutputValues: stores the output values for every test case
 *    double       errorThreshold: stores the value of the error threshold (how low the error should be before the network stops
 *                 training).
 *
 * The network class contains the following methods:
 *    void       calculateActivation(int layer, int index)
 *    void       calculateAllActivations()
 *    double     calculateError(double[] expectedValues)
 *    void       calculateLayerOfActivations(int layer)
 *    double     calculateWeightedSum(int layer, int index)
 *    double     derivativeOfErrorWithRespectToWeight(int inputNode, int outputNode, int psi)
 *    double     derivativeOfThreshold(double input)
 *    double     getActivation(int layer, int index)
 *    double[][] getActivations()
 *    double[]   getOutputActivations()
 *    double     getRandomNumberInRange(double min, double max)
 *    void       lowerErrorForAllWeights(double[] expectedValues)
 *    void       printActivations()
 *    void       printAllWeights()
 *    void       randomizeWeights(int min, int max)
 *    void       setAllWeights(Scanner sc)
 *    void       setAllExpectedOutputValues(Scanner sc)
 *    void       setInputActivation(int index, double value)
 *    void       setAllTestCases(Scanner sc)
 *    void       setWeight(int layer, int inputNodeIndex, int outputNodeIndex, double value)
 *    double     thresholdFunction(double input)
 *    void       trainNetwork()
 *    void       main(String[] args)
 *
 */
public class Network
{
    private double[][] activations;
    private double[][][] weights;
    private int[] activationArraySizes;
    private int numLayers;
    private double learningFactor;
    private double minimumWeightValue;
    private double maximumWeightValue;
    private int maxIterations;
    private double[][] omegas;
    private double[][] thetas;
    private double[][] testCases;
    private int numTestCases;
    private double[][] expectedOutputValues;
    private double errorThreshold;

    /**
     * Creates a Network object that takes in the number of input nodes, the number of hidden layer nodes, and the
     * number of output nodes and initializes arrays of activations and weights. The network also declares
     * an array of activation array sizes and an int to store the number of layers to avoid calling the .length method.
     *
     * @param inputNodes       the number of nodes in the first activation layer in the network. This number is inputted by the
     *                         user.
     * @param hiddenLayerNodes the number of nodes in each hidden layer, with hiddenLayerNodes.length representing
     *                         the number of layers. This number is inputted by the user.
     * @param outputNodes      the number of nodes in the output layer in the network. This number is inputted by the user.
     * @param lambda           the value of the learning factor used to minimize the error. This number is inputted by the user.
     * @param maxIterations    the maximum number of iterations the network will run when minimizing error
     * @param numTestCases     the number of test cases that the network will have
     * @param minWeightValue   the minimum value that a randomly generated weight can be.
     * @param maxWeightValue   the maximum value that a randomly generated weight can be.
     * @param threshold        the value of the error threshold (how low the error should be before the network stops training).
     *
     */
    public Network(int inputNodes, int[] hiddenLayerNodes, int outputNodes, double lambda, int maxIterations, int numTestCases,
                   double minWeightValue, double maxWeightValue, double threshold)
    {
        numLayers = hiddenLayerNodes.length + 2;   // sets the number of layers in the network

        activationArraySizes = new int[numLayers]; // sets the size, or the number of layers, in the activationArraySizes array

        activationArraySizes[0] = inputNodes;      // sets the number of nodes in the first layer to inputNodes

        for (int n = 1; n < numLayers - 1; n++)
        {
            activationArraySizes[n] = hiddenLayerNodes[n - 1]; // sets the sizes of the hidden layers
        }


        activationArraySizes[numLayers - 1] = outputNodes;     // sets the number of nodes in the final layer to outputNodes

        activations = new double[numLayers][];                 // sets the number of layers of the activations array
        omegas = new double[numLayers][];                      // sets the number of layers in the omegas array
        thetas = new double[numLayers][];                      // sets the number of layers in the thetas array

        for (int n = 0; n < numLayers; n++)
        {
            activations[n] = new double[activationArraySizes[n]]; // sets the number of nodes in each activation layer
            omegas[n] = new double[activationArraySizes[n]];      // sets the number of nodes in each omega layer
            thetas[n] = new double[activationArraySizes[n]];      // sets the number of nodes in each theta layer
        }

        weights = new double[numLayers - 1][][];

        for (int n = 0; n < numLayers - 1; n++)
        {
            weights[n] = new double[activationArraySizes[n]][activationArraySizes[n + 1]];
        }

        learningFactor = lambda;                                      // sets the learning factor to lambda

        this.maxIterations = maxIterations;                           // sets the maximum number of iterations

        testCases = new double[numTestCases][inputNodes];             // sets the size of the test case array
        this.numTestCases = numTestCases;                             // sets the number of test cases
        expectedOutputValues = new double[numTestCases][outputNodes]; // sets the size of the expected output array

        minimumWeightValue = minWeightValue;                          // sets the minimum value of a random weight
        maximumWeightValue = maxWeightValue;                          // sets the maximum value of a random weight

        errorThreshold = threshold;                                   // sets the error threshold

    } // public Network

    /**
     * gets the activation array
     *
     * @return the 2d array of activations
     */
    public double[][] getActivations()
    {
        return activations;
    } // public double[][] getActivations


    /**
     * returns the value stored in a specific activation
     *
     * @param layer the layer that the requested activation is in
     * @param index the index of the requested activation
     * @return the value stored in the activation
     */
    public double getActivation(int layer, int index)
    {
        return activations[layer][index];
    } // public double getActivation

    /**
     * passes a value through the sigmoid function f(x) = 1 / 1 + e^-x
     *
     * @param input the value of the input in the function
     * @return the output of the function
     */
    public double thresholdFunction(double input)
    {
        return 1.0 / (1.0 + Math.exp(- input));
    } // public double thresholdFunction

    /**
     * returns the derivative of the sigmoid threshold function at a value "input"
     *
     * @param input the value of the input in the function
     * @return the derivative of the sigmoid function at "input"
     */
    public double derivativeOfThreshold(double input)
    {
        return thresholdFunction(input) * (1.0 - thresholdFunction(input));
    } // public double derivativeOfThreshold

    /**
     * calculates the weighted sum of a specific activation by taking a weighted sum of all the values in the layer before
     *
     * @param layer the layer that the requested activation is in
     * @param index the index of the requested activation
     * @return the weighted sum of the requested activation
     */
    public double calculateWeightedSum(int layer, int index)
    {
        double sum = 0.0; // defines a variable that stores the value of the weighted sum

        for (int node = 0; node < activationArraySizes[layer - 1]; node++)
        {
            sum += activations[layer - 1][node] * weights[layer - 1][node][index]; // adds the dot product to the sum
        }
        return sum;
    } // public double calculateWeightedSum

    /**
     * calculates the value of a specific activation by taking the weighted sum of all the values in the layer before and passing
     * it through the threshold function. Also calculates the corresponding theta with each activation.
     *
     * @param layer the layer that the requested activation is in
     * @param index the index of the requested activation
     */
    public void calculateActivation(int layer, int index)
    {
        double weightedSum = calculateWeightedSum(layer, index);    // calculates the weighted sum

        thetas[layer][index] = weightedSum;                         // sets the theta of each activation to the weighted sum

        activations[layer][index] = thresholdFunction(weightedSum); // sets the activation to thresholdFunction(weightedSum)
    } // public void calculateActivation

    public double getWeight(int n, int inputNodeIndex, int outputNodeIndex)
    {
        return weights[n][inputNodeIndex][outputNodeIndex];
    }

    /**
     * calculates the error (defined as E = (1/2) * (expectedValue - outputNode)^2) for a particular training set
     *
     * @param testCase the test case that you're calculating the error for
     * @return the error of the training set
     */
    public double calculateError(int testCase)
    {
        double totalError = 0.0; // stores the sum of the errors of all the output values

        for (int outputNode = 0; outputNode < activationArraySizes[numLayers - 1]; outputNode++)
        {
            double difference = expectedOutputValues[testCase][outputNode] - activations[numLayers - 1][outputNode];
            totalError += difference * difference;
        }

        return (0.5) * totalError;
    } // public double calculateError

    /**
     * sets the value of a specific weight to a specified value
     *
     * @param layer           the layer that the requested weight is in
     * @param inputNodeIndex  the index of the input of the weight
     * @param outputNodeIndex the index of the output of the weight
     * @param value           the value that the weight is set to
     */
    public void setWeight(int layer, int inputNodeIndex, int outputNodeIndex, double value)
    {
        weights[layer][inputNodeIndex][outputNodeIndex] = value;
    } // public void setWeight

    /**
     * sets a specific input activation to a specified value, as well as the corresponding values in the first layer
     * of the theta array
     *
     * @param index the index of the input activation
     * @param value the value that the activation is set to
     */
    public void setInputActivation(int index, double value)
    {
        activations[0][index] = value;
        thetas[0][index] = value;
    } // public void setInputActivation

    public void setAllInputActivations(int testCase)
    {
        for (int node = 0; node < activationArraySizes[0]; node++) // iterates through each input node in the test case
        {
            setInputActivation(node, testCases[testCase][node]);
        }
    }

    /**
     * returns a double in the range min < x < max
     *
     * @param min the lower bounds of the range
     * @param max the upper bounds of the range
     * @return a random double between min and max
     */
    public static double getRandomNumberInRange(double min, double max)
    {
        return Math.random() * (max - min) + min;
    } // public static double getRandomNumberInRange

    /**
     * sets all the weights in the network to values read by the scanner
     *
     * @param sc the scanner that reads the values of the weights from the .txt file
     */
    public void setAllWeights(Scanner sc)
    {
        for (int n = 0; n < numLayers - 1; n++) // iterates through the layers
        {
            for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++) // iterates through input node indices
            {
                for (int outputNode = 0; outputNode < activationArraySizes[n + 1]; outputNode++) // iterates through output node indices
                {
                    setWeight(n, inputNode, outputNode, sc.nextDouble());
                }
            } // for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++)
        } // for (int n = 0; n < numLayers - 1; n++)
    } // public void setAllWeights

    /**
     * sets the values in one specific test case
     * @param sc
     * @param index the test case that you're setting values for
     */
    public void setTestCase(Scanner sc, int index)
    {
        for (int node = 0; node < activationArraySizes[0]; node++) // iterates through the input nodes
        {
            double value = sc.nextDouble();
            testCases[index][node] = value;
            // expectedOutputValues[testCase][node] = value;
        }
    }

    public void setTestCaseAndOutputValue(Scanner sc, int index)
    {
        for (int node = 0; node < activationArraySizes[0]; node++) // iterates through the input nodes
        {
            double value = sc.nextDouble();
            testCases[index][node] = value;
            expectedOutputValues[index][node] = value;
        }
    }
    public void setTestCaseValue(int testCase, int index, double value)
    {
        testCases[testCase][index] = value;
    }
    /**
     * sets the values of the test cases in the network to values read by the scanner
     *
     * @param sc the scanner that reads the values of the test cases from the .txt file
     */
    public void setAllTestCases(Scanner sc)
    {
        for (int testCase = 0; testCase < numTestCases; testCase++) // iterates through the test cases
        {
            setTestCase(sc, testCase);
        } // for (int testCase = 0; testCase < numTestCases; testCase++)
    } // public void setAllTestCases

    public void setAllTestCasesAndOutputValues(Scanner sc)
    {
        for (int testCase = 0; testCase < numTestCases; testCase++) // iterates through the test cases
        {
            setTestCaseAndOutputValue(sc, testCase);
        } // for (int testCase = 0; testCase < numTestCases; testCase++)
    }

    public void setExpectedOutputValues(Scanner sc, int index)
    {
        for (int outputNode = 0; outputNode < activationArraySizes[numLayers - 1]; outputNode++) // iterates through output nodes
        {
            expectedOutputValues[index][outputNode] = sc.nextDouble();
        }
    }

    /**
     * sets the values of the expected output values in the network read by the scanner
     *
     * @param sc the scanner that reads the values of the expected output values from the .txt file
     */
    public void setAllExpectedOutputValues(Scanner sc)
    {
        for (int testCase = 0; testCase < numTestCases; testCase++) // iterates through the test cases
        {
            setExpectedOutputValues(sc, testCase);
        } // for (int testCase = 0; testCase < numTestCases; testCase++)
    } // public void setAllExpectedOutputValues



    /**
     * prints out all the weights in the network
     */
    public void printAllWeights()
    {
        for (int n = 0; n < numLayers - 1; n++) // iterates through the layers
        {
            for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++) // iterates through input node indices
            {
                for (int outputNode = 0; outputNode < activationArraySizes[n + 1]; outputNode++) // iterates through output node indices
                {
                    System.out.println("w[" + n + "][" + inputNode + "][" + outputNode + "] = "
                            + weights[n][inputNode][outputNode]);
                }
            } // for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++)
        } // for (int n = 0; n < numLayers - 1; n++)
    } // public void printAllWeights

    /**
     * outputs the weights to a text file
     */
    public void outputWeightsToTextFile() throws Exception
    {
        try
        {
            PrintWriter out = new PrintWriter(new FileWriter("weights"));
            for (int n = 0; n < numLayers - 1; n++) // iterates through the layers
            {
                for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++) // iterates through input node indices
                {
                    for (int outputNode = 0; outputNode < activationArraySizes[n + 1]; outputNode++) // iterates through output node indices
                    {
                        out.println(weights[n][inputNode][outputNode]);
                    }
                } // for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++)
            } // for (int n = 0; n < numLayers - 1; n++)
            out.close();
        }
        catch (IOException e)
        {
            throw new IOException("IOException found");
        }
    } // public void printAllWeights



    /**
     * randomizes the weights in the network to a double in the range lowerBound to upperBound
     *
     * @param min the minimum possible value of the weight
     * @param max the maximum possible value of the weight
     */
    public void randomizeWeights(double min, double max)
    {
        for (int n = 0; n < numLayers - 1; n++) // iterates through the layers
        {
            for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++) // iterates through input node indices
            {
                for (int outputNode = 0; outputNode < activationArraySizes[n + 1]; outputNode++) // iterates through output node indices
                {
                    setWeight(n, inputNode, outputNode, getRandomNumberInRange(min, max));
                }
            } // for (int inputNode = 0; inputNode < activationArraySizes[n]; inputNode++)
        } // for (int n = 0; n < numLayers - 1; n++)
    } // public void randomizeWeights

    /**
     * calculates all the activations in a layer
     * @param layer the layer that you want to calculate the activations for
     */
    public void calculateLayerOfActivations(int layer)
    {
        for (int node = 0; node < activationArraySizes[layer]; node++) // iterates through the nodes in the given layer
        {
            calculateActivation(layer, node);
        }
    } // public void calculateLayerOfActivations

    /**
     * prints all the activations in the network
     */
    public void printActivations()
    {
        for (int n = 0; n < numLayers; n++) // iterates from the first hidden layer to the output layer
        {
            for (int node = 0; node < activationArraySizes[n]; node++) // iterates through all the nodes in a layer
            {
                System.out.println("a[" + n + "][" + node + "] = " + activations[n][node]);
            }
        }
    } // public void printActivations

    /**
     * Propagates forward through the network, calculating all activations in the network from the first hidden layer to the
     * output layer by taking the weighted sum of the nodes and weights in the layer before, and passing it through a threshold
     * function. Sets the corresponding theta value of each node to the same weighted sum
     */
    public void calculateAllActivations()
    {
        for (int n = 1; n < numLayers; n++) // iterates from the first hidden layer to the output layer
        {
            for (int node = 0; node < activationArraySizes[n]; node++) // iterates through all the nodes in a layer
            {
                double weightedSum = calculateWeightedSum(n, node); // calculates the weighted sum

                thetas[n][node] = weightedSum;
                activations[n][node] = thresholdFunction(weightedSum);
            } // for (int node = 0; node < activationArraySizes[n]; node++)
        } // for (int n = 1; n < numLayers; n++)
    } // public void calculateAllActivations

    /**
     * calculates the derivative of the error function with respect to a given weight
     * @param layer the layer that the requested weight is in
     * @param node  the index of the activation
     * @param psi   the psi value that is used to calculate the derivative
     * @return the derivative of the error function with respect to the requested weight
     */
    public double derivativeOfErrorWithRespectToWeight(int layer, int node, double psi)
    {
        return -activations[layer][node] * psi;
    } // public double derivativeOfErrorWithRespectToWeight

    /**
     * lowers the error of every single weight in the network by taking the negative gradient of the error function with respect
     * to each individual weight, multiplied by the learning factor. This method utilizes back propagation to optimize the
     * learning process
     * @param expectedValues the expected output values of the network
     */
    public void lowerErrorForAllWeights(double[] expectedValues)
    {
        for (int outputNode = 0; outputNode < activationArraySizes[numLayers - 1]; outputNode++) // iterates through the output node indices
        {
            omegas[numLayers - 1][outputNode] = expectedValues[outputNode] - activations[numLayers - 1][outputNode];
        }

        for (int n = numLayers - 2; n >= 0; n--) // iterates backwards through the layers starting from the final hidden layer
        {
            for (int prevLayerNode = 0; prevLayerNode < activationArraySizes[n + 1]; prevLayerNode++) // iterates through the previous layer
            {
                double psi = omegas[n + 1][prevLayerNode] * derivativeOfThreshold(thetas[n + 1][prevLayerNode]);

                omegas[n + 1][prevLayerNode] = 0.0; // resets the omega value in the previous layer

                for (int currentLayerNode = 0; currentLayerNode < activationArraySizes[n]; currentLayerNode++)
                {
                    /*
                     * Adds the part to every omega in the layer dependent on the
                     * weight[n][currentLayerNode][prevLayerNode]. Then lowers the error with respect to that weight. Note that
                     * the network does not calculate the omega for each node completely at once, but rather it iterates through
                     * the layer and adds to every single omega in the layer each weight at a time, so that it can immediately
                     * update the weights afterwards on the fly once it is past the dependencies. This is part of the back
                     * propagation algorithm used to optimize training for the network.
                     */
                    omegas[n][currentLayerNode] += psi * weights[n][currentLayerNode][prevLayerNode];
                    weights[n][currentLayerNode][prevLayerNode] += -learningFactor *
                            derivativeOfErrorWithRespectToWeight(n, currentLayerNode, psi);

                } // for (int currentLayerNode = 0; currentLayerNode < activationArraySizes[n]; currentLayerNode++)

            } // for (int prevLayerNode = 0; prevLayerNode < activationArraySizes[n + 1]; prevLayerNode++)

        } // for (int n = 0; n < numLayers - 1; n++)

    } // public void lowerErrorForAllWeights

    /**
     * trains the network on all the test cases until one of two conditions are satisfied: the average error of all the test cases
     * is lower than the threshold error value, or if the number of iterations reaches the maximum number of iterations allowed.
     * The method also echos all the hyperparameters, as well as the error of each test case, the average error of the test cases
     * after each iteration, and the final average error after the network finishes training
     */
    public void trainNetwork()
    {
        double averageError = Double.MAX_VALUE; // sets the average error to the max double value in order to enter the while loop

        int iterations = 0;

        randomizeWeights(minimumWeightValue, maximumWeightValue);

        while (iterations < maxIterations && averageError > errorThreshold)
        {
            iterations++;

            averageError = 0;

            for (int testCase = 0; testCase < numTestCases; testCase++) // iterates through the test cases
            {
                for (int node = 0; node < activationArraySizes[0]; node++) // iterates through each input node in the test case
                {
                    setInputActivation(node, testCases[testCase][node]);
                }

                calculateAllActivations();

                lowerErrorForAllWeights(expectedOutputValues[testCase]); // lowers the error by the learning factor

                double error = calculateError(testCase);

                for (int outputNode = 0; outputNode < activationArraySizes[numLayers - 1]; outputNode++)
                {
                    System.out.println("expected value " + (outputNode + 1) + " for test case " + (testCase + 1) +
                            " = " + expectedOutputValues[testCase][outputNode]);
                    System.out.println("actual value " + (outputNode + 1) + " for test case " + (testCase + 1) +
                            " = " + activations[numLayers - 1][outputNode]);
                }
                System.out.println("error for test case " + (testCase + 1) + " = " + error + "\n");

                averageError += error; // adds the error of this test case to averageError (summing the errors of all the test cases)

            } // for (int testCase = 0; testCase < numTestCases; testCase++)

            averageError /= (double) numTestCases;

            System.out.println("AVERAGE ERROR FOR ITERATION " + iterations + " = " + averageError + "\n");

        } // while (iterations < maxIterations || averageError > 0.01)


        System.out.println("HYPER PARAMETERS");
        for (int n = 0; n < numLayers; n++)
        {
            System.out.println("num of activations in layer " + n + " = " + activationArraySizes[n]);
        }
        System.out.println("error threshold = " + errorThreshold);
        System.out.println("max iterations = " + maxIterations);
        System.out.println("min weight value = " + minimumWeightValue);
        System.out.println("max weight value = " + maximumWeightValue);
        System.out.println("learning factor = " + learningFactor + "\n");

        System.out.println("FINAL ERROR = " + averageError + "\n"); // prints out the final error after the training has stopped
    } // public void trainNetwork

    /**
     * returns an array of the output activations in the network
     * @return the output activations
     */
    public double[] getOutputActivations()
    {
        return activations[numLayers - 1];
    } // public double[] getOutputActivations()

    /**
     * Main method that takes in user input from the .txt file and creates
     *
     * @param args the string array of arguments from the command line
     * @throws IOException if the input/output operations fail or are interrupted
     */
    public static void main(String[] args) throws Exception
    {
        try
        {
            Scanner fileNameScanner = new Scanner(System.in);
            System.out.println("Name of input parameters file? Enter nothing to use default file name 'testdoc.txt'");
            String filename = fileNameScanner.nextLine();

            if (filename.length() == 0) // if no value is entered, uses default file name "testdoc.txt"
            {
                filename = "out/production/NeuralNetworks/com/company/testdoc.txt";
                System.out.println("---XOR Test---");
            }
            FileInputStream inStream = new FileInputStream(new File(filename));

            Scanner sc = new Scanner(inStream);

            int inputNodes = sc.nextInt();                  // reads the number of input nodes

            int[] hiddenLayerNodes = new int[sc.nextInt()]; // reads the number of hidden layers

            for (int node = 0; node < hiddenLayerNodes.length; node++)
            {
                hiddenLayerNodes[node] = sc.nextInt();
            }

            int outputNodes = sc.nextInt();           // reads the number of output nodes

            int numTestCases = sc.nextInt();          // reads the number of test cases

            double lambda = sc.nextDouble();          // reads the learning factor value

            double minWeightValue = sc.nextDouble();  // reads the minimum random weight value

            double maxWeightValue = sc.nextDouble();  // reads the maximum random weight value

            double threshold = sc.nextDouble();       // reads the error threshold value

            int maxIterations = sc.nextInt();         // reads the maximum iterations value

            Network network = new Network(inputNodes, hiddenLayerNodes, outputNodes, lambda, maxIterations, numTestCases,
                    minWeightValue, maxWeightValue, threshold);

            network.setAllTestCases(sc);            // sets the test cases

            network.setAllExpectedOutputValues(sc);

            network.trainNetwork();              // trains the network

        } // try
        catch (IOException e)
        {
            throw new IOException("IOException found");
        }
    } // public static void main
} // public class Network