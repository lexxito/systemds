/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.tugraz.sysds.runtime.instructions.cp;

import java.util.List;

import org.tugraz.sysds.runtime.DMLRuntimeException;
import org.tugraz.sysds.runtime.controlprogram.context.ExecutionContext;
import org.tugraz.sysds.runtime.lineage.LineageItem;
import org.tugraz.sysds.runtime.lineage.LineageItemUtils;
import org.tugraz.sysds.runtime.lineage.LineageTraceable;
import org.tugraz.sysds.runtime.matrix.data.MatrixBlock;
import org.tugraz.sysds.runtime.matrix.operators.Operator;

public class MatrixBuiltinNaryCPInstruction extends BuiltinNaryCPInstruction implements LineageTraceable {

	protected MatrixBuiltinNaryCPInstruction(Operator op, String opcode, String istr, CPOperand output, CPOperand[] inputs) {
		super(op, opcode, istr, output, inputs);
	}

	@Override
	public void processInstruction(ExecutionContext ec) {
		//separate scalars and matrices and pin all input matrices
		List<MatrixBlock> matrices = ec.getMatrixInputs(inputs);
		List<ScalarObject> scalars = ec.getScalarInputs(inputs);
		
		MatrixBlock outBlock = null;
		if( "cbind".equals(getOpcode()) || "rbind".equals(getOpcode()) ) {
			boolean cbind = "cbind".equals(getOpcode());
			outBlock = matrices.get(0).append(matrices.subList(1, matrices.size())
				.toArray(new MatrixBlock[0]), new MatrixBlock(), cbind);
		}
		
		else if( "nmin".equals(getOpcode()) || "nmax".equals(getOpcode()) ) {
			outBlock = MatrixBlock.naryOperations(_optr, matrices.toArray(new MatrixBlock[0]),
				scalars.toArray(new ScalarObject[0]), new MatrixBlock());
		}
		else {
			throw new DMLRuntimeException("Unknown opcode: "+getOpcode());
		}
		
		//release inputs and set output matrix or scalar
		ec.releaseMatrixInputs(inputs);
		if( output.getDataType().isMatrix() ) {
			ec.setMatrixOutput(output.getName(), outBlock);
		}
		else {
			ec.setVariable(output.getName(), ScalarObjectFactory.createScalarObject(
				output.getValueType(), outBlock.quickGetValue(0, 0)));
		}
	}
	
	@Override
	public LineageItem[] getLineageItems(ExecutionContext ec) {
		return new LineageItem[]{new LineageItem(output.getName(),
			getOpcode(), LineageItemUtils.getLineage(ec, inputs))};
	}
}
