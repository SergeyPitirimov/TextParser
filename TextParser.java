/**
How to start a program from the console:
java -classpath /home/pitirimov/Javaworks/TextParser/dist/TextParser.jar textparser.TextParser 
 /home/pitirimov/Javaworks/TextParser/one.txt /home/pitirimov/Javaworks/TextParser/second.txt
*/

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextParser
{
  private static Logger logger = LoggerFactory.getLogger(TextParser.class);

  public static void main(String[] args)
  {
//    int textFileCounter = args.length;
    int textFileCounter = 3;
    String[] arguments = {"/home/pitirimov/Javaworks/TextParser/one.txt", "/home/pitirimov/Javaworks/TextParser/two.txt", "/home/pitirimov/Javaworks/TextParser/three.txt"};
    Thread[] newThread = new SimpleThread[textFileCounter];
    final Object lock = new Object();
    AtomicBoolean duplicateWordHasBeenFound = new AtomicBoolean(false);

    /* Check for text files as arguments */
    if (textFileCounter == 0)
    {
      logger.info("Usage: java -cp /path_to/TextParser.jar textparser.TextParser filename1 filename2 ...");
      return;
    }

    /* Create the linked list of unique words */
    LinkedList<String> linkedList = new LinkedList<String>();

    /* Create new thread for each text file */
    while (textFileCounter != 0)
    {
      /* Create the thread with text file name as argument */
      newThread[textFileCounter - 1] = new SimpleThread(arguments[textFileCounter - 1], linkedList, lock, duplicateWordHasBeenFound);

      /* Start the new thread */
      newThread[textFileCounter - 1].start();

      textFileCounter = textFileCounter - 1;      
    }

    /* Wait until all threads have done their tasks */
    try
    {
      for (int i = 0; i < textFileCounter; i++)
      {
        newThread[i].join();
      }
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }
}

/* This is the thread body */
class SimpleThread extends Thread
{
  private static Logger logger = LoggerFactory.getLogger(SimpleThread.class);
  private final String textFileName;
  private LinkedList<String> threadLinkedList;
  private final Object threadLock;
  private volatile AtomicBoolean threadDuplicateWordHasBeenFound;
  
  /* This is the constructor with text file name as argument */
  public SimpleThread(final String threadTextFileName, LinkedList<String> threadLinkedList, final Object threadLock, AtomicBoolean threadDuplicateWordHasBeenFound)
  {
    this.textFileName = threadTextFileName;
    this.threadLinkedList = threadLinkedList;
    this.threadLock = threadLock;
    this.threadDuplicateWordHasBeenFound = threadDuplicateWordHasBeenFound;
  }
  
  public void run()
  {
    final int maxFileSize = 10000000;
    byte[] text = new byte[maxFileSize];
    int bytesToRead = 0, readBytes = 0;

    /* Read the text from file */
    try
    {
      /* Open the file for reading */
      FileInputStream textFile = new FileInputStream(textFileName);
      
      bytesToRead = textFile.available();
      
      if (bytesToRead > maxFileSize)
      {
        bytesToRead = maxFileSize; 
      }
      
      /* Read the file */
      while (bytesToRead > 0)
      {
        readBytes = textFile.read(text, readBytes, bytesToRead);
        bytesToRead -= readBytes;
      }

      /* Close the file */
      textFile.close();      
    }
    catch (FileNotFoundException e)
    {
      logger.info("File " + textFileName + "not found!");
    }
    catch (IOException e)
    {
      logger.info("I/O exception with file " + textFileName);
    }

    /* Get the string, divide it on several words and put to the linked list, shared between threads */
    try
    {
      /* Transform the byte array to string */
      String textString = new String(text, 0, readBytes);
      
      /* Check for foreign symbols */
      for (int i = 0; i < textString.length(); i++)
      {
        char currentSymbol = textString.charAt(i);

        if ((currentSymbol >= 'A' && currentSymbol <= 'Z') || (currentSymbol >= 'a' && currentSymbol <= 'z'))
        {
          /* Foreign symbol has been detected */
          logger.info("Non cyrillic symbol has been detected.");
          return;
        }
      }

      /* Divide the string to the words, separated by spaces */
      StringTokenizer separatedWordsFromFile = new StringTokenizer(textString, "\\ |\\.|\\,|\\;|\\:|\\!|\\?");

      while (separatedWordsFromFile.hasMoreTokens() && (threadDuplicateWordHasBeenFound.get() == false))
      {
        /* COmpare the word with each word in the linked list */
        synchronized (threadLock)
        {          
          ListIterator<String> threadListIterator = threadLinkedList.listIterator(0);
          String wordToCompare = separatedWordsFromFile.nextToken();

          /* Look and compare every word in the linked list with current word from the file */
          while (threadListIterator.hasNext())
          {
            if (wordToCompare.equals(threadListIterator.next()))
            {
              threadDuplicateWordHasBeenFound.set(true);
              logger.info("The duplicated word has been found: " + wordToCompare);
              return;
            }
          }

          /* Insert the unique word to the linked list */
          threadLinkedList.addLast(wordToCompare);
        }
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      logger.info("Can't transform byte array to string in the file " + textFileName);
    }
    catch (ConcurrentModificationException e)
    {
      logger.info("Can't put the word to the thread linked list. There is concurrency.");
    }
  }
}