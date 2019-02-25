package com.bakdata.conquery.models.messages.namespaces.specific;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonIgnore;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@CPSType(id="IMPORT_BIT", base=NamespacedMessage.class)
@RequiredArgsConstructor @NoArgsConstructor @Getter @Setter
public class ImportBits extends WorkerMessage.Slow {
	@NotEmpty
	private byte[] bytes;
	@NotEmpty
	private final List<Bit> bits = new ArrayList<>();
	
	@NotEmpty @Nonnull
	private String tag;
	@Nonnull @NotNull
	private ImportId imp;
	@Nonnull @NotNull
	private TableId table;
	
	@JsonIgnore
	private transient IntSet addedEntities = new IntOpenHashSet();
	
	public void addBits(Bit bit) {
		if(!addedEntities.add(bit.getId())) {
			throw new IllegalStateException("There already was a bit for entity "+bit.getId());
		}
		bits.add(bit);
	}

	@Override
	public void react(Worker context) throws Exception {
		getProgressReporter().setMax(bits.size());
		Import imp = context.getStorage().getImport(this.imp);
		//one bit for every column(non-primary) plus one for constant size
		
		int bytePos = 0;
		
		BlockFactory factory = imp.getBlockFactory();
		
		List<Block> newBlocks = new ArrayList<>();
		
		getProgressReporter().setMax(bits.size());
		for(Bit bit:bits) {
			try(ByteArrayInputStream input = new ByteArrayInputStream(bytes, bytePos, bit.getSize())) {
				
				/*
				List<Object[]> events = new ArrayList<>(bit.getNumberOfEntries());
				
				for(int e=0;e<bit.getNumberOfEntries();e++) {
					input.readBytes(nullBytes);
					
					Object[] row = new Object[t.getColumns().length];
					for(int i=0;i<row.length;i++) {
						if(nulls.getBit(i))
							row[i] = imp.getColumns()[i].getType().kryoRead(kryo, input);
					}
					events.add(row);
				}*/
				newBlocks.add(factory.readBlock(bit.getId(), imp, input));
				if(input.available() > 0) {
					throw new IllegalStateException("After reading the bit "+bit+" there are still "+input.available()+" bytes remaining in its content");
				}
				
				bytePos += bit.getSize();
			}
			getProgressReporter().report(1);
		}
		context.getStorage().addBlocks(newBlocks);
	}
	
	@Override
	public String toString() {
		return
			"Importing "
			+ bits.size()
			+ " from "
			+ FileUtils.byteCountToDisplaySize(bytes.length)
			+ " as "
			+ tag
			+ " into "
			+ table;
	}

	@AllArgsConstructor @NoArgsConstructor @Getter @Setter @ToString
	public static class Bit {
		private int id;
		private int size;
	}
}
