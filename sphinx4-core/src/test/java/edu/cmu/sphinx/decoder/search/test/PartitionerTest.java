//package edu.cmu.sphinx.decoder.search.test;
//
//import edu.cmu.sphinx.decoder.scorer.Scoreable;
//import edu.cmu.sphinx.decoder.search.Partitioner;
//import edu.cmu.sphinx.decoder.search.Token;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.util.*;
//
//public class PartitionerTest {
//
//	public static void testSorted(Token[] tokens, int p) {
//		for (int i = 0; i < p; i++) {
//			Assert.assertTrue(tokens[i].score() >= tokens[p].score());
//		}
//		for (int i = p; i < tokens.length; i++) {
//			Assert.assertTrue(tokens[i].score() <= tokens[p].score());
//		}
//	}
//
//	private static void performTestPartitionSizes(int absoluteBeamWidth,
//												  int tokenListSize, boolean tokenListLarger) {
//
//		Random random = new Random(System.currentTimeMillis());
//
//		Token parent = new Token(null, 0);
//		Token[] tokens = new Token[tokenListSize];
//
//		for (int i = 0; i < tokens.length; i++) {
//			float logTotalScore = random.nextFloat();
//			tokens[i] = new Token(parent, null, logTotalScore, 0.0f, 0.0f, i);
//		}
//
//		final int r = Partitioner.partition(tokens, tokens.length,
//				absoluteBeamWidth);
//
//		if (tokenListLarger) {
//			Assert.assertEquals(r, absoluteBeamWidth - 1);
//		} else {
//			Assert.assertEquals (r, tokenListSize - 1);
//		}
//
//		List<Token> firstList = new LinkedList<>();
//		if (r >= 0) {
//			float lowestScore = tokens[r].score();
//
//			for (int i = 0; i <= r; i++) {
//				Assert.assertTrue(tokens[i].score() >= lowestScore);
//				firstList.add(tokens[i]);
//			}
//			for (int i = r + 1; i < tokens.length; i++) {
//				Assert.assertTrue(lowestScore > tokens[i].score());
//			}
//
//			Collections.sort(firstList, Scoreable.COMPARATOR);
//
//			List<Token> secondList = Arrays.asList(tokens);
//			Collections.sort(secondList, Scoreable.COMPARATOR);
//
//			for (Iterator<Token> i1 = firstList.iterator(), i2 = secondList
//					.iterator(); i1.hasNext() && i2.hasNext();) {
//				Token t1 = i1.next();
//				Token t2 = i2.next();
//				Assert.assertEquals(t1, t2);
//			}
//		}
//	}
//
//	@Test
//	public void testPartitionOrders() {
//		int p;
//		Token[] tokens = new Token[100000];
//
//		for (int i = 0; i < 100000; i++)
//			tokens[i] = new Token(null, null, 1 - i, 0, 0, 0);
//		p = Partitioner.partition(tokens, 100000, 3000);
//		Assert.assertEquals(p, 2999);
//		testSorted(tokens, p);
//
//		for (int i = 0; i < 100000; i++)
//			tokens[i] = new Token(null, null, i, 0, 0, 0);
//		p = Partitioner.partition(tokens, 100000, 3000);
//		Assert.assertEquals(p, 2999);
//		testSorted(tokens, p);
//
//		for (int i = 0; i < 100000; i++)
//			tokens[i] = new Token(null, null, 0, 0, 0, 0);
//		p = Partitioner.partition(tokens, 100000, 3000);
//		Assert.assertEquals(p, 2999);
//		testSorted(tokens, p);
//
//		for (int i = 0; i < 100000; i++)
//			tokens[i] = new Token(null, null, (float) Math.random(), 0, 0, 0);
//		p = Partitioner.partition(tokens, 100000, 3000);
//		Assert.assertEquals(p, 2999);
//		testSorted(tokens, p);
//	}
//
//	@Test
//	public static void testPartitionSizes() {
//
//		int absoluteBeamWidth = 1500;
//		int tokenListSize = 3000;
//
//		// Test 1 : (tokenListSize > absoluteBeamWidth)
//		performTestPartitionSizes(absoluteBeamWidth, tokenListSize, true);
//
//		// Test 2 : (tokenListSize == absoluteBeamWidth)
//		tokenListSize = absoluteBeamWidth;
//		performTestPartitionSizes(absoluteBeamWidth, tokenListSize, false);
//
//		// Test 3 : (tokenListSize < absoluteBeamWidth)
//		tokenListSize = 1000;
//		performTestPartitionSizes(absoluteBeamWidth, tokenListSize, false);
//
//		// Test 4 : (tokenListSize == 0)
//		tokenListSize = 0;
//		performTestPartitionSizes(absoluteBeamWidth, tokenListSize, false);
//	}
//}
