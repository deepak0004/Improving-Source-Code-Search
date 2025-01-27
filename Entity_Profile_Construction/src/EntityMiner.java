import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class EntityMiner {
	public static void main(String[] args) {
		int speedUp = 50;
		//int step = 0;
		if (args.length > 0) {
			try {
				speedUp = Integer.parseInt(args[0]);
				//step = Integer.parseInt(args[1]);
			} catch (Exception e) {}
		}
		mine(speedUp);
	}

	private static void mine(int speedUp) {
		try {
			long startTime = (new Date()).getTime();
//				
//			Properties props = FileUtil.loadProps();
//			if (props == null) return;			
						
			String filePath = "posts.xml";
			
			List<String> entities = FileUtil.readFromFileAsList("ProcessedDiscoveredEntities.txt");
			Map<String, Integer> collectionTF = FileUtil.getLastNIntMapFromFile("allCodeTFMaxNorm.txt", 100); //anne-exp3 has the data files.
						
			Map<String, Set<String>> skipEntities = EntityTagger.getSkipLists("skipList.txt");
			for(String entityName: entities) {
				
				//unigram processing
				System.out.println("Processing " + entityName);
				Set<String> skipEntitySet = skipEntities.get(entityName);
				Set<Integer> ids = LineRanker.extract(filePath, entityName, skipEntitySet);
				FileUtil.writeSetToFile(ids, entityName + "-relevant-post-ids.txt");
				
				List<String> code = LineBasedCodeExtractor.getAllCode(ids, filePath, speedUp); //Collect unique tokens per codeset (not per line).
				FileUtil.writeListToFile(code, entityName + "-UniqueTokensInCodeSet.txt"); 
				
				Map<String, Integer> tf = TFCalculator.construct(entityName + "-UniqueTokensInCodeSet.txt");
				FileUtil.writeMapToFile(tf, entityName + "-UniqueTokensInCodeSetTF.txt", 0);
				
				Map<String, Integer> weights = TFMaxNormalizer.normalizeMax(tf);
				FileUtil.writeMapToFile(weights, entityName + "-UniqueTokensInCodeSetTFMaxNorm.txt", 150);
							
				Map<String, Integer> normalizedCollection = TFNormalizer.normalize(tf, collectionTF);				
				FileUtil.writeMapToFile(normalizedCollection, entityName + "-NormalizedTF.txt", 0);
				
				//Ngram
				Map<String, Float> entityTF = FileUtil.getFloatMapFromFile(entityName + "-NormalizedTF.txt");
				Map<String,Integer> weightedCode = LineRanker.getAllCode(ids, filePath, entityTF); //what if we do not normalize?
				weightedCode = FileUtil.sortByValues(weightedCode);	
				FileUtil.writeMapToFile(weightedCode,entityName + "-WeightedLines.txt", 0);
				weightedCode = FileUtil.getLastNIntMapFromFile(entityName + "-WeightedLines.txt", 500);
				FileUtil.writeMapToFile(weightedCode,entityName + "-WeightedLines.txt", 0);
				
				Map<String, Float> weightedLines = FileUtil.getLastNFloatMapFromFile(entityName + "-WeightedLines.txt", 0);
				Map<String, Integer> patterns = LongestPatternMiner.findLongPatterns(filePath, ids, entityTF, weightedLines, speedUp, 0);
				FileUtil.writeMapToFile(patterns,entityName + "-Long-patterns.txt", 0); //-in-all-code
				
				Set<Integer> allIds = FileUtil.readFromFileAsSet(ConfigUtil.getInputStream("allIds.txt")); //allIds.txt
				Map<String, Integer> patternsAll = LongestPatternMiner.findLongPatterns(filePath, allIds, entityTF, weightedLines, speedUp, 5);
				FileUtil.writeMapToFile(patternsAll,entityName + "-Long-patterns-in-all-code.txt", 0); //
				
				
				Map<String, Integer> patternsMap = FileUtil.getLastNIntMapFromFile(entityName + "-Long-patterns-in-all-code.txt",100);			
				Map<String, Integer> entityPatternFreq = FileUtil.getLastNIntMapFromFile(entityName + "-Long-patterns.txt", 0);
				
				//Map<String, Integer> patternsAll1 = TFMaxNormalizer.normalizeMax(patternsMap);
				//Map<String, Integer> patterns1 = TFMaxNormalizer.normalizeMax(entityPatternFreq);
				Map<String, Integer> patternsAll1 = patternsMap; //Let us not do max norm. Max norm prevents us from distinguishing between strong patterns from weak ones.
								Map<String, Integer> patterns1 = entityPatternFreq;

				//FileUtil.writeMapToFile(patternsAll1, entityName + "-Long-patterns-in-all-code-MaxNorm.txt", 0);
				//FileUtil.writeMapToFile(patterns1, entityName + "-Long-patterns-MaxNorm.txt", 0);
				
				Map<String, Integer>  patternsNormalized = TFNormalizer.normalize(patterns1, patternsAll1);				
				FileUtil.writeMapToFile(patternsNormalized,entityName + "-Long-Normalized-1.txt", 0);
			}
			long finishTime = (new Date()).getTime();
			System.out.println("Took " + (finishTime - startTime)/60000 + " minutes.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}