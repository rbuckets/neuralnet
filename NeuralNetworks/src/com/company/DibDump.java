package com.company;

/**
 * @author EricN
 * February 2, 2009
 * A "short" code segment to open bitmaps and
 * extract the bits as an array of integers. If the array is small (less than 30 x 30)
 * it will print the hex values to the console.
 * The code subsequently saves the array as a 32-bit true color bitmap. The default input file name is test1.bmp
 * the default output name is output.bmp. You can override these defaults by passing
 * different names as arguments. This file is not meant to be used "as is". You should create your own class
 * and extract what you need from here to populate it.
 *
 * This code has a lot of magic numbers. I suggest you figure out what they are for and make properly named constants for them
 *
 * Rev: 2/18/09 - case 1: for 2 colors was missing
 *                case 2: had 2 not 4 colors.
 *                The mask for 16 colors was 1 and should have been 0x0F.
 *                case 16: for 2^16 colors was not decoding the 5 bit colors properly and did not read the padded bytes. It should work properly now. Not tested.
 *                Updated the comment on biSizeImage and all the image color depths
 *                Decoding for color table images was incorrect. All image types are padded so that the number of bytes read per
 *                   scan line is a multiple of 4. Added the code to read in the "dead bytes" along with updating the comments. Additionally
 *                   the most significant bit, half-nibble or nibble is on the left side of the least significant parts. The ordering was
 *                   reversed which scrambled the images.
 *                256 Color images now works correctly.
 *                16 Color images now works correctly.
 *                4 Color images should work, but is not tested.
 *                2 Color images now works correctly.
 *
 * Rev: 2/19/09 - The color table was not correctly read when biClrUsed was non-zero. Added one line (and comments) just prior to reading the color table
 *                   to account for this field being non-zero.
 * Rev: 2/20/09 - Added RgbQuad class
 *                Added pelToRGB(), rgbToPel() and colorToGrayscale() to DibDump class. These use the new RgbQuad class.
 *                Added peltoRGBQ(), rgbqToPel() (these handle the reserved byte in 32-bit images)
 *                Did NOT implement pelToRGB and rgbToPel in DibDump overall.
 * Rev: 2/21/09   The array index values for passing arguments in main() were 1 and 2, should have been 0 and 1 (at least according to Conrad). Not tested.
 * Rev: 11/12/14  Added the topDownDIB flag to deal with negative biHeight values which means image is stored rightside up. All loops depending on the
 *                biHeight value were modified to accommodate both inverted (normal) and top down images. The image is stored in the normal manner
 *                regardless of how it was read in.
 * Rev: 01/10/17  Was using the term 24-bit color when it was 32-bit in the comments. Fixed the documentation to be correct.
 *
 * Classes in the file:
 *  RgbQuad
 *  DibDump
 *
 * Methods in this file:
 *  int     swapInt(int v)
 *  int     swapShort(int v)
 *  RgbQuad pelToRGBQ(int pel)
 *  int     rgbqToPel(int red, int green, int blue, int reserved)
 *  RgbQuad pelToRGB(int pel)
 *  int     rgbToPel(int red, int green, int blue)
 *  int     colorToGrayscale(int pel)
 *  void    main(String[] args)
 *
 * There is a lot of cutting and pasting from various
 * documents dealing with bitmaps and I have not taken the
 * time to clean up the formatting in the comments. The C syntax is
 * included for reference. The types are declared in windows.h. The C
 * structures and data arrays are predefined static so that they don't
 * ever fall out of scope.
 *
 * I have not "javafied" this file. Much of it needs to be broken out into
 * various specialty methods. These modifications are left as an exercise
 * for the reader.
 *
 * Notes on reading bitmaps:
 *
 * The BMP format assumes an Intel integer type (little endian), however, the Java virtual machine
 * uses the Motorola integer type (big endian), so we have to do a bunch of byte swaps to get things
 * to read and write correctly. Also note that many of the values in a bitmap header are unsigned
 * integers of some kind and Java does not know about unsigned values, except for reading in
 * unsigned byte and unsigned short, but the unsigned int still poses a problem.
 * We don't do any math with the unsigned int values, so we won't see a problem.
 *
 * Bitmaps on disk have the following basic structure
 *  BITMAPFILEHEADER (may be missing if file is not saved properly by the creating application)
 *  BITMAPINFO -
 *        BITMAPINFOHEADER
 *        RGBQUAD - Color Table Array (not present for true color images)
 *  Bitmap Bits in one of many coded formats
 *
 *  The BMP image is stored from bottom to top, meaning that the first scan line in the file is the last scan line in the image.
 *
 *  For ALL images types, each scan line is padded to an even 4-byte boundary.
 *
 *  For images where there are multiple pels per byte, the left side is the high order element and the right is the
 *  low order element.
 *
 *  in Windows on a 32 bit processor...
 *  DWORD is an unsigned 4 byte integer
 *  WORD is an unsigned 2 byte integer
 *  LONG is a 4 byte signed integer
 *
 *  in Java we have the following sizes:
 *
 * byte
 *   1 signed byte (two's complement). Covers values from -128 to 127.
 *
 * short
 *   2 bytes, signed (two's complement), -32,768 to 32,767
 *
 * int
 *   4 bytes, signed (two's complement). -2,147,483,648 to 2,147,483,647.
 *   Like all numeric types ints may be cast into other numeric types (byte, short, long, float, double).
 *   When lossy casts are done (e.g. int to byte) the conversion is done modulo the length of the smaller type.
 */
import com.company.Network;

import java.io.*;
import java.util.Scanner;

/*
 * A member-variable-only class for holding the RGBQUAD C structure elements.
 */
final class RgbQuad
{
    int red;
    int green;
    int blue;
    int reserved;
}

public class DibDump
{
    // BITMAPFILEHEADER
    static int bmpFileHeader_bfType;          // WORD
    static int bmpFileHeader_bfSize;          // DWORD
    static int bmpFileHeader_bfReserved1;     // WORD
    static int bmpFileHeader_bfReserved2;     // WORD
    static int bmpFileHeader_bfOffBits;       // DWORD
    // BITMAPINFOHEADER
    static int bmpInfoHeader_biSize;          // DWORD
    static int bmpInfoHeader_biWidth;         // LONG
    static int bmpInfoHeader_biHeight;        // LONG
    static int bmpInfoHeader_biPlanes;        // WORD
    static int bmpInfoHeader_biBitCount;      // WORD
    static int bmpInfoHeader_biCompression;   // DWORD
    static int bmpInfoHeader_biSizeImage;     // DWORD
    static int bmpInfoHeader_biXPelsPerMeter; // LONG
    static int bmpInfoHeader_biYPelsPerMeter; // LONG
    static int bmpInfoHeader_biClrUsed;       // DWORD
    static int bmpInfoHeader_biClrImportant;  // DWORD
    // The true color pels
    static int[][] imageArray;

    // if bmpInfoHeader_biHeight is negative then the image is a top down DIB. This flag is used to
// identify it as such. Note that when the image is saved, it will be written out in the usual
// inverted format with a positive bmpInfoHeader_biHeight value.
    static boolean topDownDIB = false;
    /*
     * Methods to go between little and big endian integer formats.
     */
    public int swapInt(int v)
    {
        return  (v >>> 24) | (v << 24) | ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
    }

    public int swapShort(int v)
    {
        return  ((v << 8) & 0xFF00) | ((v >> 8) & 0x00FF);
    }
    /*
     * Method pelToRGBQ accepts an integer (32 bit) picture element and returns the red, green and blue colors.
     * Unlike pelToRGB, this method also extracts the most significant byte and populates the reserved element of RgbQuad.
     * It returns an RgbQuad object. See rgbqToPel(int red, int green, int blue, int reserved) to go the the other way.
     */
    public RgbQuad pelToRGBQ(int pel)
    {
        RgbQuad rgbq = new RgbQuad();

        rgbq.blue     =  pel        & 0x00FF;
        rgbq.green    = (pel >> 8)  & 0x00FF;
        rgbq.red      = (pel >> 16) & 0x00FF;
        rgbq.reserved = (pel >> 24) & 0x00FF;

        return rgbq;
    }

    /*
     * The rgbqToPel method takes red, green and blue color values plus an additional byte and returns a single 32-bit integer color.
     * See pelToRGBQ(int pel) to go the other way.
     */
    public int rgbqToPel(int red, int green, int blue, int reserved)
    {
        return (reserved << 24) | (red << 16) | (green << 8) | blue;
    }

    /*
     * Method pelToRGB accepts an integer (32 bit) picture element and returns the red, green and blue colors
     * as an RgbQuad object. See rgbToPel(int red, int green, int blue) to go the the other way.
     */
    public RgbQuad pelToRGB(int pel)
    {
        RgbQuad rgb = new RgbQuad();

        rgb.reserved = 0;

        rgb.blue  =  pel        & 0x00FF;
        rgb.green = (pel >> 8)  & 0x00FF;
        rgb.red   = (pel >> 16) & 0x00FF;

        return rgb;
    }

    /*
     * The rgbToPel method takes red, green and blue color values and returns a single 32-bit integer color.
     * See pelToRGB(int pel) to go the other way.
     */
    public int rgbToPel(int red, int green, int blue)
    {
        return (red << 16) | (green << 8) | blue;
    }

    /*
     * Y = 0.3RED+0.59GREEN+0.11Blue
     * The colorToGrayscale method takes a color picture element (pel) and returns the gray scale pel using just one of may possible formulas
     */
    public int colorToGrayscale(int pel)
    {
        RgbQuad rgb = pelToRGB(pel);

        int lum = (int)Math.round(0.3 * (double)rgb.red + 0.589 * (double)rgb.green + 0.11 * (double)rgb.blue);
        return rgbToPel(lum, lum, lum);
    }
    /*
     *
     * ---- MAIN ----
     *
     */
    public static void main(String[] args) throws Exception
    {
        String inFileName, outFileName, outFileName2;
        int i, j, k;
        int numberOfColors;
        int pel;
        int iByteVal, iColumn, iBytesPerRow, iPelsPerRow, iTrailingBits, iDeadBytes;
// RBGQUAD
        int rgbQuad_rgbBlue;
        int rgbQuad_rgbGreen;
        int rgbQuad_rgbRed;
        int rgbQuad_rgbReserved;           // not used in this method
// The color table
        int[] colorPallet = new int[256];  // reserve space for the largest possible color table

        DibDump dibdumper = new DibDump(); // needed to get to the byte swapping methods

        // creates a scanner that reads the user-inputted file name
        try
        {
            Scanner fileNameScanner = new Scanner(System.in);
            boolean isTraining = true;
            System.out.println("train or test?");
            if (fileNameScanner.nextLine().equals("test"))
            {
                isTraining = false;
            }

            System.out.println("number of pixels?");
            int numPixels = fileNameScanner.nextInt();
            fileNameScanner.nextLine();

            System.out.println("Name of input parameters file? Enter nothing to use default file name 'bitmaptest'");
            String filename = fileNameScanner.nextLine();
            // if no value is entered, uses default file name "bitmaptest"
            if (filename.length() == 0)
            {
                if (isTraining == true)
                {
                    filename = "src/com/company/bitmaptrain";
                }
                else
                {
                    filename = "src/com/company/bitmaptest";
                }
            }
            FileInputStream inStream = new FileInputStream(new File(filename));

            // creates a scanner to read the .txt file using the FileInputStream
            Scanner sc = new Scanner(inStream);

            int numTestCases = 0;
            double lambda = 0.0;
            double minWeightValue = 0.0;
            double maxWeightValue = 0.0;
            double threshold = 0.0;
            int maxIterations = 0;

            // reads the number of output nodes
            int outputNodes = 5;

            // reads the number of hidden layers
            int[] hiddenLayerNodes = new int[sc.nextInt()];

            for (int node = 0; node < hiddenLayerNodes.length; node++) // iterates through the number of hidden layers
            {
                hiddenLayerNodes[node] = sc.nextInt();
            }

            numTestCases = sc.nextInt();

            // reads the number of test cases
            if (isTraining)
            {
                // reads the learning factor value
                lambda = sc.nextDouble();

                // reads the minimum random weight value
                minWeightValue = sc.nextDouble();

                // reads the maximum random weight value
                maxWeightValue = sc.nextDouble();

                // reads the error threshold value
                threshold = sc.nextDouble();

                // reads the maximum iterations value
                maxIterations = sc.nextInt();
            }


            // creates the network using all of the gathered values
            Network network = new Network(numPixels, hiddenLayerNodes, outputNodes, lambda, maxIterations, numTestCases,
                    minWeightValue, maxWeightValue, threshold);

            for (int testCase = 0; testCase < numTestCases; testCase++)
            {
                if (isTraining)
                {
                    inFileName =
                            "src/com/company/TrainingData/trainingimage" + testCase + ".bmp";
                }
                else
                {
                    inFileName = "src/com/company/TestData/testimage" + testCase + ".bmp";
                }
                FileInputStream fstream = new FileInputStream(inFileName);
                DataInputStream in = new DataInputStream(fstream);
                bmpFileHeader_bfType      = dibdumper.swapShort(in.readUnsignedShort());    // WORD
                bmpFileHeader_bfSize      = dibdumper.swapInt(in.readInt());                // DWORD
                bmpFileHeader_bfReserved1 = dibdumper.swapShort(in.readUnsignedShort());    // WORD
                bmpFileHeader_bfReserved2 = dibdumper.swapShort(in.readUnsignedShort());    // WORD
                bmpFileHeader_bfOffBits   = dibdumper.swapInt(in.readInt());                // DWORD

                bmpInfoHeader_biSize          = dibdumper.swapInt(in.readInt());              // DWORD
                bmpInfoHeader_biWidth         = dibdumper.swapInt(in.readInt());              // LONG
                bmpInfoHeader_biHeight        = dibdumper.swapInt(in.readInt());              // LONG
                bmpInfoHeader_biPlanes        = dibdumper.swapShort(in.readUnsignedShort());  // WORD
                bmpInfoHeader_biBitCount      = dibdumper.swapShort(in.readUnsignedShort());  // WORD
                bmpInfoHeader_biCompression   = dibdumper.swapInt(in.readInt());              // DWORD
                bmpInfoHeader_biSizeImage     = dibdumper.swapInt(in.readInt());              // DWORD
                bmpInfoHeader_biXPelsPerMeter = dibdumper.swapInt(in.readInt());              // LONG
                bmpInfoHeader_biYPelsPerMeter = dibdumper.swapInt(in.readInt());              // LONG
                bmpInfoHeader_biClrUsed       = dibdumper.swapInt(in.readInt());              // DWORD
                bmpInfoHeader_biClrImportant  = dibdumper.swapInt(in.readInt());              // DWORD

// Since we use the height to crate arrays, it cannot have a negative a value. If the height field is
// less than zero, then make it positive and set the topDownDIB flag to TRUE so we know that the image is
// stored on disc upsidedown (which means it is actually rightside up).
                if (bmpInfoHeader_biHeight < 0)
                {
                    topDownDIB = true;
                    bmpInfoHeader_biHeight = -bmpInfoHeader_biHeight;
                }

                switch (bmpInfoHeader_biBitCount) // Determine the number of colors in the default color table
                {
                    case 1:
                        numberOfColors = 2;
                        break;
                    case 2:
                        numberOfColors = 4;
                        break;
                    case 4:
                        numberOfColors = 16;
                        break;
                    case 8:
                        numberOfColors = 256;
                        break;
                    default:
                        numberOfColors = 0; // no color table
                }

                if (bmpInfoHeader_biClrUsed > 0) numberOfColors = bmpInfoHeader_biClrUsed;

                for (i = 0; i < numberOfColors; ++i) // Read in the color table (or not if numberOfColors is zero)
                {
                    rgbQuad_rgbBlue      = in.readUnsignedByte(); // lowest byte in the color
                    rgbQuad_rgbGreen     = in.readUnsignedByte();
                    rgbQuad_rgbRed       = in.readUnsignedByte(); // highest byte in the color
                    rgbQuad_rgbReserved  = in.readUnsignedByte();

                    // Build the color from the RGB values. Since we declared the rgbQuad values to be int, we can shift and then OR the values
                    // to build up the color. Since we are reading one byte at a time, there are no "endian" issues.

                    colorPallet[i] = (rgbQuad_rgbRed << 16) | (rgbQuad_rgbGreen << 8) | rgbQuad_rgbBlue;
                } // for (i = 0; i < numberOfColors; ++i)

                imageArray = new int[bmpInfoHeader_biHeight][bmpInfoHeader_biWidth]; // Create the array for the pels
                /*
                 * I use the same loop structure for each case for clarity so you can see the similarities and differences.
                 * The outer loop is over the rows (in reverse), the inner loop over the columns.
                 */
                iPelsPerRow = bmpInfoHeader_biWidth;
                iDeadBytes = (4 - (iPelsPerRow * 3) % 4) % 4;
                for (int row = 0; row < bmpInfoHeader_biHeight; ++row) // read over the rows
                {
                    if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;

                    for (j = 0; j < iPelsPerRow; ++j)         // j is now just the column counter
                    {
                        rgbQuad_rgbBlue      = in.readUnsignedByte();
                        rgbQuad_rgbGreen     = in.readUnsignedByte();
                        rgbQuad_rgbRed       = in.readUnsignedByte();
                        pel = (rgbQuad_rgbRed << 16) | (rgbQuad_rgbGreen << 8) | rgbQuad_rgbBlue;
                        imageArray[i][j] = pel;
                    }
                    for (j = 0; j < iDeadBytes; ++j) in.readUnsignedByte(); // Now read in the "dead bytes" to pad to a 4 byte boundary
                }

                int counter = 0;
                for (int row = 0; row < imageArray.length; row++)
                {
                    for (int column = 0; column < imageArray[row].length; column++)
                    {
                        network.setTestCaseValue(testCase, counter,
                                dibdumper.pelToRGB(dibdumper.colorToGrayscale(imageArray[row][column])).blue / 255.0);
                        counter++;
                    }
                }
                if (isTraining)
                {
                    network.setExpectedOutputValues(sc, testCase);
                }
                in.close();
                fstream.close();
            }

            if (isTraining)
            {
                network.trainNetwork();
                network.outputWeightsToTextFile();
            }
            else
            {
                FileInputStream in3 = new FileInputStream(new File("weights"));
                Scanner weightscan = new Scanner(in3);
                network.setAllWeights(weightscan);

                for (int testCase = 0; testCase < numTestCases; testCase++) // iterates through the test cases
                {
                    network.setAllInputActivations(testCase);

                    network.calculateAllActivations();
                    double[] outputs = network.getOutputActivations();

                    double maxValue = outputs[0];
                    int maxIndex = 1;
                    for (int outputValue = 0; outputValue < outputNodes; outputValue++)
                    {
                        System.out.println((outputValue + 1) + ": " + outputs[outputValue]);
                        if (outputs[outputValue] > maxValue)
                        {
                            maxValue = outputs[outputValue];
                            maxIndex = outputValue + 1;
                        }
                    }
                    System.out.println("testimage" + testCase + " is: " + maxIndex);
                    System.out.println("\n");
                }
            }



// there is no color table for this true color image, so write out the pels
        }
        catch (Exception e)
        {
            System.err.println("File input error " + e);
        }
    } // public static void main
} // public class DibDump




