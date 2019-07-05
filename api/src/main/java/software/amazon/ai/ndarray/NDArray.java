/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.ai.ndarray;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import software.amazon.ai.Context;
import software.amazon.ai.ndarray.index.NDIndex;
import software.amazon.ai.ndarray.internal.NDArrayEx;
import software.amazon.ai.ndarray.types.DataDesc;
import software.amazon.ai.ndarray.types.DataType;
import software.amazon.ai.ndarray.types.Layout;
import software.amazon.ai.ndarray.types.Shape;
import software.amazon.ai.ndarray.types.SparseFormat;
import software.amazon.ai.training.GradReq;

/**
 * An interface representing an n-dimensional array.
 *
 * <p>NDArray is the core data structure for all mathematical computations. An NDArray represents a
 * multidimensional, fixed-size homogeneous array. It has very similar behaviour to the Numpy python
 * package with the addition of efficient computing.
 */
public interface NDArray extends AutoCloseable {

    /**
     * Returns the {@link NDScopedFactory} used to create the {@code NDArray}.
     *
     * @return {@link NDScopedFactory}
     */
    NDScopedFactory getFactory();

    /**
     * Returns the {@link DataType} of the {@code NDArray}.
     *
     * <p>{@link DataType} is a definition of the precision level of the {@code NDArray}. All values
     * inside the same NDArray would have the same data type.
     *
     * @return {@link DataType}
     */
    DataType getDataType();

    /**
     * Returns the {@link Context} of the {@code NDArray}.
     *
     * <p>{@link Context} class contains the information where this NDArray stored in memory, like
     * CPU/GPU.
     *
     * @return {@link Context}
     */
    Context getContext();

    /**
     * Returns the {@link Shape} of the {@code NDArray}.
     *
     * <p>{@link Shape} defines how this NDArray is represented multi-dimensionally.
     *
     * @return the {@link Shape} of the {@code NDArray}.
     */
    Shape getShape();

    /**
     * Returns the {@link Layout} of the {@code NDArray}.
     *
     * <p>{@link Layout} defines the meaning of each dimension in the array.
     *
     * @return {@link Layout}
     */
    Layout getLayout();

    /**
     * Returns the {@link DataDesc} of the {@code NDArray}.
     *
     * <p>{@link DataDesc} contains all information about NDArray, including {@link Context}, {@link
     * DataType}, {@link Shape}, {@link Layout}, {@link
     * software.amazon.ai.ndarray.types.SparseFormat}.
     *
     * @return {@link Layout}
     */
    DataDesc getDataDescriptor();

    /**
     * Returns {@code true} if this array is a {@link SparseNDArray}.
     *
     * @return {@code true} if this array is a {@link SparseNDArray}
     */
    boolean isSparse();

    /**
     * Attaches this NDArray to specified NDScopedFactory.
     *
     * <p>Attached resource will be closed when the factory is closed.
     *
     * @param factory {@link NDScopedFactory} to be attached
     */
    default void attach(NDScopedFactory factory) {
        detach();
        getFactory().attach(factory);
    }

    /**
     * Detaches this NDArray from current NDScopedFactory's lifecycle.
     *
     * <p>This NDArray becomes un-managed, it's user's responsibility to close the NDArray. Failed
     * to close the resource has to wait on GC to be freed, and might cause out of native memory.
     */
    default void detach() {
        getFactory().detach(this);
    }

    /**
     * Converts the NDArray to a different {@link Context}.
     *
     * @param ctx {@link Context} to be set
     * @param copy set {@code true} if you want to return a copy of the Existing {@code NDArray}.
     * @return the result {@code NDArray} with the new {@link Context}
     */
    NDArray asInContext(Context ctx, boolean copy);

    /**
     * Converts the NDArray to a different {@link DataType}.
     *
     * @param dtype {@link DataType} to be set
     * @param copy set {@code true} if you want to return a copy of the Existing NDArray
     * @return the result {@code NDArray} with the new {@link DataType}
     */
    NDArray asType(DataType dtype, boolean copy);

    /**
     * Converts the array into a 2D Matrix.
     *
     * @return This NDArray as Matrix
     * @throws IllegalStateException Thrown if the NDArray is not a 2D matrix
     */
    Matrix asMatrix();

    /** Computes the gradients of the NDArray w.r.t variables. */
    void backward();

    /**
     * Computes the gradients of the NDArray w.r.t variables.
     *
     * @param retainGraph Whether to retain the computation graph for another backward pass on the
     *     same graph. By default the computation history is cleared.
     * @param isTraining Whether to compute gradient for training or inference.
     */
    void backward(boolean retainGraph, boolean isTraining);

    /**
     * Computes the gradients of the NDArray w.r.t variables.
     *
     * @param outGrad Gradient with respect to head
     * @param retainGraph Whether to retain the computation graph for another backward pass on the
     *     same graph. By default the computation history is cleared.
     * @param isTraining Whether to compute gradient for training or inference.
     */
    void backward(NDArray outGrad, boolean retainGraph, boolean isTraining);

    /**
     * Attaches a gradient buffer to this NDArray, so that `backward` can compute the gradient with
     * respect to it.
     */
    void attachGrad();

    /**
     * Attaches a gradient buffer to this NDArray, so that `backward` can compute the gradient with
     * respect to it.
     *
     * @param gradReq {@link GradReq} How gradient will be accumulated.
     * @param sparseFormat {@link SparseFormat} The storage type of the gradient array. Defaults to
     *     the same type of this {@code NDArray}.
     */
    void attachGrad(GradReq gradReq, SparseFormat sparseFormat);

    /**
     * Returns the gradient buffer attached to this {@code NDArray}.
     *
     * @return the gradient buffer attached to this {@code NDArray}.
     */
    NDArray getGradient();

    /**
     * Returns the encoding format of the NDArray, or null.
     *
     * @return the encoded NDArray
     */
    byte[] getEncoded();

    /**
     * Encodes NDArray to an {@link OutputStream}.
     *
     * @param os OutputStream
     * @throws IOException for writing problems
     */
    default void encode(OutputStream os) throws IOException {
        os.write(getEncoded());
    }

    /**
     * Converts this NDArray to a double array.
     *
     * @return a double array
     */
    double[] toDoubleArray();

    /**
     * Converts this NDArray to a float array.
     *
     * @return a float array
     */
    float[] toFloatArray();

    /**
     * Converts this NDArray to a int array.
     *
     * @return a int array
     */
    int[] toIntArray();

    /**
     * Converts this NDArray to a long array.
     *
     * @return a long array
     */
    long[] toLongArray();

    /**
     * Converts this NDArray to a long array.
     *
     * @return a long array
     */
    byte[] toByteArray();

    /**
     * Converts this NDArray to a Number array based on its data type.
     *
     * @return a Number array
     */
    default Number[] toArray() {
        switch (getDataType()) {
            case FLOAT32:
                float[] floatArray = toFloatArray();
                return IntStream.range(0, floatArray.length)
                        .mapToObj(i -> floatArray[i])
                        .toArray(Number[]::new);
            case FLOAT64:
                return Arrays.stream(toDoubleArray()).boxed().toArray(Double[]::new);
            case INT32:
                return Arrays.stream(toIntArray()).boxed().toArray(Integer[]::new);
            case INT64:
                return Arrays.stream(toLongArray()).boxed().toArray(Long[]::new);
            default:
                throw new IllegalStateException("Unsupported DataType: " + getDataType());
        }
    }

    /**
     * Sets the NDArray value from {@link Buffer}.
     *
     * @param data The input buffered data
     */
    void set(Buffer data);

    /**
     * Sets the NDArray value from an array of floats.
     *
     * @param data array of floats to set
     */
    void set(float[] data);

    /**
     * Sets the NDArray value from an array of ints.
     *
     * @param data array of integers to set
     */
    void set(int[] data);

    /**
     * Sets the NDArray value from an array of doubles.
     *
     * @param data array of doubles to set
     */
    void set(double[] data);

    /**
     * Sets the NDArray value from an array of longs.
     *
     * @param data array of longs to set
     */
    void set(long[] data);

    /**
     * Sets the NDArray value from an array of bytes.
     *
     * @param data array of bytes to set
     */
    void set(byte[] data);

    /**
     * Sets the specified index in a new NDArray with the given values.
     *
     * @param index The locations to update
     * @param value The value to replace with. Can broadcast if given a smaller dimensions than the
     *     index
     * @return a new NDArray with the updated values
     */
    NDArray set(NDIndex index, NDArray value);

    /**
     * Sets the specified index in a new NDArray with the given value.
     *
     * @param index The locations to update
     * @param value The value to replace with
     * @return a new NDArray with the updated values
     */
    NDArray set(NDIndex index, Number value);

    /**
     * Sets the specified index in a new NDArray with the given value.
     *
     * @param index The single index to update
     * @param value The value to replace with
     * @return a new NDArray with the updated value
     * @throws IllegalArgumentException Thrown if the index does not correspond to a single element
     */
    NDArray setElement(NDIndex index, Number value) throws IllegalArgumentException;

    /**
     * Sets the specified index in the NDArray with the given values.
     *
     * @param index The locations to update
     * @param value The value to replace with. Can broadcast if given a smaller dimensions than the
     *     index
     * @return the updated NDArray
     */
    NDArray seti(NDIndex index, NDArray value);

    /**
     * Sets the specified index in the NDArray with the given value.
     *
     * @param index The locations to update
     * @param value The value to replace with
     * @return the updated NDArray
     */
    NDArray seti(NDIndex index, Number value);

    /**
     * Sets the specified index in the NDArray with the given value.
     *
     * @param index The single index to update
     * @param value The value to replace with
     * @return the updated NDArray
     * @throws IllegalArgumentException Thrown if the index does not correspond to a single element
     */
    NDArray setElementi(NDIndex index, Number value) throws IllegalArgumentException;

    /**
     * Returns a partial {@code NDArray}.
     *
     * @param index the section of the NDArray to return
     * @return the partial NDArray
     */
    NDArray get(NDIndex index);

    /**
     * Returns a partial {@code NDArray}.
     *
     * @param indices Indices to indicate what to get
     * @return the partial NDArray
     * @see NDIndex#NDIndex(String)
     */
    default NDArray get(String indices) {
        return get(new NDIndex(indices));
    }

    /**
     * Returns a partial {@code NDArray}.
     *
     * @param indices indices with each index corresponding to the dimensions and negative indices
     *     tarting from the end
     * @return the partial NDArray
     */
    default NDArray get(int... indices) {
        return get(new NDIndex(indices));
    }

    /**
     * Returns a zero dimensional NDArray corresponding to a single element.
     *
     * @param indices The index of the element to return. Must return only a single element.
     * @return a zero dimensional NDArray corresponding to the element.
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default NDArray getElement(int... indices) {
        NDArray value = get(new NDIndex(indices));
        if (value.size() != 1) {
            throw new IllegalArgumentException("The supplied Index does not produce an element");
        }
        return value;
    }

    /**
     * Returns an element from the {@code NDArray}.
     *
     * @param indices the index
     * @return The element in the specified index as a long
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default long getLong(int... indices) {
        return getElement(indices).toLongArray()[0];
    }

    /**
     * Returns an element from the {@code NDArray}.
     *
     * @param indices the index
     * @return The element in the specified index as a double
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default double getDouble(int... indices) {
        return getElement(indices).toDoubleArray()[0];
    }

    /**
     * Returns an element from the {@code NDArray}.
     *
     * @param indices the index
     * @return The element in the specified index as a float
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default float getFloat(int... indices) {
        return getElement(indices).toFloatArray()[0];
    }

    /**
     * Returns an element from the {@code NDArray}.
     *
     * @param indices the index
     * @return The element in the specified index as a float
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default int getInt(int... indices) {
        return getElement(indices).toIntArray()[0];
    }

    /**
     * Returns an element from the {@code NDArray}.
     *
     * @param indices the index
     * @return The element in the specified index as a float
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default byte getByte(int... indices) {
        return getElement(indices).toByteArray()[0];
    }

    /**
     * Returns an element from the {@code NDArray}.
     *
     * @param indices the index
     * @return The element in the specified index as a float
     * @throws IllegalArgumentException Thrown if the result is not a single element
     */
    default int getUint8(int... indices) {
        return getByte(indices) & 0xff;
    }

    /**
     * Copies the current NDArray value to the one passed in.
     *
     * @param array the NDArray prepared to be copied to
     */
    void copyTo(NDArray array);

    /**
     * Returns a copy of this NDArray.
     *
     * @return a copy of this NDArray
     */
    NDArray dup();

    /**
     * Returns an array of zeros with the same {@link Shape}, {@link DataType} and {@link
     * SparseFormat} as the input array.
     *
     * @return {@code NDArray} filled with zeros
     */
    NDArray zerosLike();

    /**
     * Returns an array of ones with the same {@link Shape}, {@link DataType} and {@link
     * SparseFormat} as the input array.
     *
     * @return {@code NDArray} filled with ones
     */
    NDArray onesLike();

    /**
     * Returns uninitialized array with the same dtype/order/shape as this one.
     *
     * @return the result {@code NDArray}
     */
    NDArray like();

    ////////////////////////////////////////
    // Operators
    ////////////////////////////////////////

    /**
     * Performs an indirect sort of the NDArray ascending on the last dimension.
     *
     * @return an array of indices corresponding to elements in the NDArray on the axis, the output
     *     DataType is always {@link DataType#INT32}
     * @see NDArray#argsort(int, boolean)
     */
    default NDArray argsort() {
        return argsort(-1, true);
    }

    /**
     * Performs an indirect sort of the NDArray ascending on the given dimension.
     *
     * @param axis the axis to sort along
     * @return an array of indices corresponding to elements in the NDArray on the axis, the output
     *     DataType is always {@link DataType#INT32}
     * @see NDArray#argsort(int, boolean)
     */
    default NDArray argsort(int axis) {
        return argsort(axis, true);
    }

    /**
     * Performs an indirect sort of the NDArray on the given dimension.
     *
     * @param axis the axis to sort along
     * @param ascending whether to sort ascending
     * @return an array of indices corresponding to elements in the NDArray on the axis, the output
     *     DataType is always {@link DataType#INT32}
     */
    NDArray argsort(int axis, boolean ascending);

    /**
     * Returns a sorted copy of an input array along the given axis.
     *
     * @param axis axis along which to sort.
     * @return return sorted NDArray
     */
    NDArray sort(int axis);

    /**
     * Returns a sorted copy of an flattened input array.
     *
     * @return return sorted NDArray
     */
    NDArray sort();

    /**
     * Returns the softmax over the entire array.
     *
     * @return the softmax over the entire array
     * @see <a href="https://en.wikipedia.org/wiki/Softmax_function">softmax</a>
     * @see NDArray#softmax(int[], double)
     */
    default NDArray softmax() {
        return softmax(new int[0]);
    }

    /**
     * Returns the softmax on the specified axis.
     *
     * @param axis the axis to sort along, -1 for the last axis
     * @return the softmax
     * @see <a href="https://en.wikipedia.org/wiki/Softmax_function">softmax</a>
     * @see NDArray#softmax(int[], double)
     */
    default NDArray softmax(int axis) {
        return softmax(new int[] {axis});
    }

    /**
     * Returns the softmax on the specified axis.
     *
     * @param axis the axis to sort along, -1 for the last axis
     * @param temperature the exponent multiplier Beta in the softmax.
     * @return the softmax
     * @see <a href="https://en.wikipedia.org/wiki/Softmax_function">softmax</a>
     * @see NDArray#softmax(int[], double)
     */
    default NDArray softmax(int axis, double temperature) {
        return softmax(new int[] {axis}, temperature);
    }

    /**
     * Returns the softmax across the specified axes.
     *
     * @param axes the axes to compute the softmax of. An empty array indicates computing the
     *     softmax for the whole array
     * @return the softmax
     * @see <a href="https://en.wikipedia.org/wiki/Softmax_function">softmax</a>
     */
    NDArray softmax(int[] axes);

    /**
     * Returns the softmax across the specified axes.
     *
     * @param axes the axes to compute the softmax of. An empty array indicates computing the
     *     softmax for the whole array
     * @param temperature The exponent multiplier Beta in the softmax.
     * @return the softmax
     * @see <a href="https://en.wikipedia.org/wiki/Softmax_function">softmax</a>
     * @see NDArray#softmax(int[], double)
     */
    NDArray softmax(int[] axes, double temperature);

    /**
     * Splits the array into size(0) new NDArrays along the first dimension.
     *
     * @return Given this array has shape [n, a, b, c, ...], returns an NDList of n arrays of shape
     *     [a, b, c, ...]
     * @see NDArray#split(int, boolean)
     */
    default NDList split() {
        return split(0, true);
    }

    /**
     * Splits the array into size(axis) new NDArrays along the given dimension.
     *
     * @param axis The axis to split along
     * @return an NDList with size(axis) NDArrays with shape {@code this.shape.remove(axis) }
     * @see NDArray#split(int, boolean)
     */
    default NDList split(int axis) {
        return split(axis, true);
    }

    /**
     * Splits the array into size(axis) new NDArrays along the given dimension.
     *
     * @param axis The axis to split along
     * @param squeezeAxis whether to remove the specified output from the output NDArrays or leave
     *     as size 1
     * @return an NDList with size(axis) NDArrays with shape {@code squeezeAxis ?
     *     this.shape.remove(axis) : this.shape.set(axis, 1)}
     * @see NDArray#split(int, boolean)
     */
    NDList split(int axis, boolean squeezeAxis);

    /**
     * Splits the array into a given number of new NDArrays along the given dimension.
     *
     * @param axis The axis to split along
     * @param numOutputs The number of NDArrays to split into. This must equally divide the length
     *     of the axis.
     * @return an NDList with numOutputs NDArrays with shape {@code (this.shape.axis /= axis) }
     * @throws IllegalArgumentException thrown if the numOutputs does not equally divide the given
     *     axis
     */
    NDList split(int axis, int numOutputs);

    /**
     * Returns the cumulative sum along a axis. In-place method.
     *
     * @param axis axis along which the cumulative sum is computed.
     * @return this object
     */
    NDArray cumsumi(int axis);

    /**
     * Returns the cumulative sum over the flattened array. In-place method.
     *
     * @return this object
     */
    NDArray cumsumi();

    /**
     * Returns the cumulative sum along a axis.
     *
     * @param axis axis along which the cumulative sum is computed.
     * @return the cumulative sum along the specified dimension
     */
    NDArray cumsum(int axis);

    /**
     * Returns the cumulative sum over the flattened array.
     *
     * @return the cumulative sum along the specified dimension
     */
    NDArray cumsum();

    /**
     * Returns the binary NDArray for "Epsilon equals" comparison.
     *
     * @param other the number to compare
     * @return the binary NDArray for "Epsilon equals" comparison
     */
    NDArray eps(Number other);

    /**
     * Returns the binary NDArray for "Epsilon equals" comparison.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Epsilon equals" comparison
     */
    NDArray eps(NDArray other);

    /**
     * Returns the binary NDArray for "Equals" comparison.
     *
     * @param other the number to compare
     * @return the binary NDArray for "Equals" comparison
     */
    NDArray eq(Number other);

    /**
     * Returns the binary NDArray for "Equals" comparison.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Equals" comparison
     */
    NDArray eq(NDArray other);

    /**
     * Returns the boolean {@code true} iff all elements in the NDArray are equal to the other
     * {@code NDArray}.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Equals" comparison
     */
    boolean contentEquals(NDArray other);

    /**
     * Returns the boolean {@code true} iff all elements in the NDArray are equal to the {@code
     * number}.
     *
     * @param number the number to compare
     * @return the binary NDArray for "Equals" comparison
     */
    boolean contentEquals(Number number);

    /**
     * Returns the binary NDArray for "Not equals" comparison.
     *
     * @param other the number to compare
     * @return the binary NDArray for "Not equals" comparison
     */
    NDArray neq(Number other);

    /**
     * Returns the binary NDArray for "Not equals" comparison.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Not equals" comparison
     */
    NDArray neq(NDArray other);

    /**
     * Returns the binary NDArray for "Greater" comparison.
     *
     * @param other the number to compare
     * @return the binary NDArray for "Greater" comparison
     */
    NDArray gt(Number other);

    /**
     * Returns the binary NDArray for "Greater Than" comparison.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Greater Than" comparison
     */
    NDArray gt(NDArray other);

    /**
     * Returns binary NDArray for "Greater or equals" comparison.
     *
     * @param other the NDArray to compare
     * @return binary NDArray for "Greater or equals" comparison
     */
    NDArray gte(Number other);

    /**
     * Returns binary NDArray for "Greater or equals" comparison.
     *
     * @param other the number to compare
     * @return binary NDArray for "Greater or equals" comparison
     */
    NDArray gte(NDArray other);

    /**
     * Returns the binary NDArray for "Less or equals" comparison.
     *
     * @param other the number to compare
     * @return the binary NDArray for "Less or equals" comparison
     */
    NDArray lte(Number other);

    /**
     * Returns the binary NDArray for "Less" comparison.
     *
     * @param other the number to compare
     * @return the binary NDArray for "Less" comparison
     */
    NDArray lt(Number other);

    /**
     * Returns the binary NDArray for "Less or equals" comparison.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Less or equals" comparison
     */
    NDArray lte(NDArray other);

    /**
     * Returns the binary NDArray for "Less" comparison.
     *
     * @param other the NDArray to compare
     * @return the binary NDArray for "Less" comparison
     */
    NDArray lt(NDArray other);

    /**
     * Returns the binary NDArray with value {@code true} where this array's entries are infinite,
     * or {@code false} where they are not infinite.
     *
     * @return the binary array with value {@code true} if the array's entries are infinite
     */
    NDArray isInfinite();

    /**
     * Returns the binary NDArray with value {@code true} where this array's entries are NaN, or
     * {@code false} where they are not NaN.
     *
     * @return the binary array with value {@code true} if the array's entries are NaN
     */
    NDArray isNaN();

    /**
     * Returns the NDArray negative (cloned).
     *
     * @return Array copy with all values negated
     */
    NDArray neg();

    /**
     * Sets the negative version of this NDArray in place.
     *
     * @return this array with all values negated
     */
    NDArray negi();

    /**
     * Divides an array by a number.
     *
     * @param n Number to divide values by
     * @return copy of array after division
     */
    NDArray div(Number n);

    /**
     * In place scalar division of an array.
     *
     * @param n Number to divide values by
     * @return this array after applying division operation
     */
    NDArray divi(Number n);

    /**
     * Scalar multiplication of an array (copy).
     *
     * @param n the number to multiply by
     * @return a copy of this NDArray multiplied by the given number
     */
    NDArray mul(Number n);

    /**
     * In place scalar multiplication of an array.
     *
     * @param n The number to multiply by
     * @return this array after applying scalar multiplication
     */
    NDArray muli(Number n);

    /**
     * Scalar subtraction of an array (copied).
     *
     * @param n the number to subtract by
     * @return Copy of this array after applying subtraction operation
     */
    NDArray sub(Number n);

    /**
     * In place scalar subtraction of an array.
     *
     * @param n Number to subtract
     * @return this array after applying subtraction operation
     */
    NDArray subi(Number n);

    /**
     * Adds a number to each element of the array.
     *
     * @param n the number to add
     * @return the result of the addition
     */
    NDArray add(Number n);

    /**
     * Adds a number to each element of the array in place.
     *
     * @param n the number to add
     * @return the result of the addition
     */
    NDArray addi(Number n);

    /**
     * Adds (broadcasting) another NDArray to this {@code NDArray}.
     *
     * @param other the other NDArray to add
     * @return the result of the addition
     */
    NDArray add(NDArray other);

    /**
     * Adds (broadcasting) another NDArray to this NDArray in place.
     *
     * @param other the other NDArray to add
     * @return the result of the addition
     */
    NDArray addi(NDArray other);

    /**
     * Return a mask on whether each element matches the given index.
     *
     * @param index the index of values to set to true.
     * @return new boolean NDArray where values are {@code true} if it matches the index
     */
    NDArray createMask(NDIndex index);

    /**
     * Return a mask on whether each element matches the given condition.
     *
     * @param predicate a predicate to apply to each element of the array
     * @return new boolean NDArray where values are {@code true} if it matches the predicate
     */
    NDArray createMask(Predicate<Number> predicate);

    /**
     * Repeats the array in tiles a given number of times.
     *
     * @param repeats the number of times to repeat for each dimension
     * @return a NDArray that has been tiled
     */
    NDArray tile(long repeats);

    /**
     * Repeats the array in tiles a given number of times along the given axis.
     *
     * @param axis the axis to repeat
     * @param repeats the number of times to repeat for each dimension
     * @return an NDArray that has been tiled
     * @throws IllegalArgumentException Thrown for invalid axis
     */
    NDArray tile(int axis, long repeats);

    /**
     * Repeats the array in tiles a given number of times.
     *
     * @param repeats the number of times to repeat along each axis
     * @return an NDArray that has been tiled
     */
    NDArray tile(long[] repeats);

    /**
     * Repeats the array in tiles a given number of times to match the desired shape.
     *
     * <p>If the desired shape has fewer dimensions that the array, it will tile against the final
     * dimensions.
     *
     * @param desiredShape the shape that should be converted to
     * @return an NDArray that has been tiled
     */
    NDArray tile(Shape desiredShape);

    /**
     * Repeats each array element a given number of times.
     *
     * @param repeats the number of times to repeat for each dimension
     * @return an NDArray that has been tiled
     */
    NDArray repeat(long repeats);

    /**
     * Repeats each array element a given number of times along the given axis.
     *
     * @param axis the axis to repeat
     * @param repeats the number of times to repeat for each dimension
     * @return an NDArray that has been tiled
     * @throws IllegalArgumentException Thrown for invalid axis
     */
    NDArray repeat(int axis, long repeats);

    /**
     * Repeats each array element a given number of times for each axis.
     *
     * @param repeats the number of times to repeat along each axis
     * @return an NDArray that has been tiled
     */
    NDArray repeat(long[] repeats);

    /**
     * Repeats each array element to match the desired shape.
     *
     * <p>If the desired shape has fewer dimensions that the array, it will tile against the final
     * dimensions.
     *
     * @param desiredShape the shape that should be converted to
     * @return an NDArray that has been tiled
     */
    NDArray repeat(Shape desiredShape);

    /**
     * Perform a copy matrix multiplication.
     *
     * @param other the other matrix to perform matrix multiply with
     * @return the result of the matrix multiplication
     */
    NDArray mmul(NDArray other);

    /**
     * Copy (element wise) division of two NDArrays.
     *
     * @param other the second NDArray to divide
     * @return the result of the divide
     */
    NDArray div(NDArray other);

    /**
     * copy (element wise) multiplication of two NDArrays.
     *
     * @param other the second NDArray to multiply
     * @return the result of the addition
     */
    NDArray mul(NDArray other);

    /**
     * copy subtraction of two NDArrays.
     *
     * @param other the second NDArray to subtract
     * @return the result of the addition
     */
    NDArray sub(NDArray other);

    /**
     * in place (element wise) division of two NDArrays.
     *
     * @param other the second NDArray to divide
     * @return the result of the divide
     */
    NDArray divi(NDArray other);

    /**
     * Performs in place (element wise) multiplication of two NDArrays.
     *
     * @param other the second NDArray to multiply
     * @return the result of the multiplication
     */
    NDArray muli(NDArray other);

    /**
     * Performs in place (element wise) subtraction of two NDArrays.
     *
     * @param other the second NDArray to subtract
     * @return the result of the subtraction
     */
    NDArray subi(NDArray other);

    /**
     * Returns the maximum (absolute) value in this NDArray, along the specified dimensions.
     *
     * @param dimension the dimension to getScalar the mean along
     * @return the mean along the specified dimension of this NDArray
     */
    NDArray amax(int... dimension);

    /**
     * Returns the maximum (absolute) value in this NDArray.
     *
     * @return Max absolute value
     */
    Number amaxNumber();

    /**
     * Returns the minimum (absolute) value in this NDArray, along the specified dimensions.
     *
     * @param dimension the dimension to getScalar the absolute min along
     * @return Minimum absolute value
     */
    NDArray amin(int... dimension);

    /**
     * Returns the absolute min value in this NDArray.
     *
     * @return Absolute min value
     */
    Number aminNumber();

    /**
     * Finds the max of all elements in the {@code NDArray}.
     *
     * @return the max
     */
    Number max();

    /**
     * Finds the max over the given axes.
     *
     * @param axes the axes to find the max over
     * @return an NDArray with the specified axes removed from the Shape containing the max
     * @see NDArray#max(int[], boolean)
     */
    default NDArray max(int[] axes) {
        return max(axes, false);
    }

    /**
     * Finds the max over the given axes.
     *
     * @param axes the axes to find the max over
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return an NDArray after the max
     */
    NDArray max(int[] axes, boolean keepDims);

    /**
     * Finds the min of all elements in the {@code NDArray}.
     *
     * @return the min
     */
    Number min();

    /**
     * Finds the min over the given axes.
     *
     * @param axes the axes to find the min over
     * @return an NDArray with the specified axes removed from the Shape containing the min
     * @see NDArray#min(int[], boolean)
     */
    default NDArray min(int[] axes) {
        return min(axes, false);
    }

    /**
     * Finds the min over the given axes.
     *
     * @param axes the axes to find the min over
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return an NDArray after the min
     */
    NDArray min(int[] axes, boolean keepDims);

    /**
     * Sums all elements in the {@code NDArray}.
     *
     * @return the sum
     */
    Number sum();

    /**
     * Sums over the given axes.
     *
     * @param axes the axes to sum over
     * @return an NDArray with the specified axes removed from the Shape containing the sum
     * @see NDArray#sum(int[], boolean)
     */
    default NDArray sum(int[] axes) {
        return sum(axes, false);
    }

    /**
     * Sums over the given axes.
     *
     * @param axes the axes to sum over
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return an NDArray after the sum
     */
    NDArray sum(int[] axes, boolean keepDims);

    /**
     * Finds the product of all elements in the {@code NDArray}.
     *
     * @return the product
     */
    Number prod();

    /**
     * Finds the product over the given axes.
     *
     * @param axes the axes to prod over
     * @return an NDArray with the specified axes removed from the Shape containing the prod
     * @see NDArray#prod(int[], boolean)
     */
    default NDArray prod(int[] axes) {
        return prod(axes, false);
    }

    /**
     * Finds the product over the given axes.
     *
     * @param axes the axes to prod over
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return an NDArray after the prod
     */
    NDArray prod(int[] axes, boolean keepDims);

    /**
     * Finds the mean of all elements in the {@code NDArray}.
     *
     * @return the mean
     */
    Number mean();

    /**
     * Finds the mean over the given axes.
     *
     * @param axes the axes to find the mean over
     * @return an NDArray with the specified axes removed from the Shape containing the mean
     * @see NDArray#mean(int[], boolean)
     */
    default NDArray mean(int[] axes) {
        return mean(axes, false);
    }

    /**
     * Finds the mean over the given axes.
     *
     * @param axes the axes to find the mean over
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return an NDArray after the mean
     */
    NDArray mean(int[] axes, boolean keepDims);

    /**
     * Flattens the array into a 1D NDArray in row-major order.
     *
     * <p>To flatten in column-major order, first transpose the NDArray
     *
     * @return 1 1D NDArray of equal size
     */
    NDArray flatten();

    /**
     * Reshapes the NDArray to the given shape.
     *
     * <p>You can reshape it to match another NDArray by calling {@code a.reshape(b.getShape()) }
     *
     * @param shape the shape to reshape into. Must have equal size to the current shape.
     * @return a reshaped NDArray
     * @throws IllegalArgumentException Thrown if the given shape does not match the size of the
     *     current shape
     */
    NDArray reshape(Shape shape);

    /**
     * Expand the shape of a {@code NDArray}.
     *
     * <p>Insert a new axis that will appear at the axis position in the expanded
     *
     * @param axis position in the expanded axes where the new axis is placed.
     * @return output array. The number of dimensions is one greater than that of the input array.
     */
    NDArray expandDims(int axis);

    /**
     * Joins a sequence of {@code NDArray} along a new axis.
     *
     * <p>The axis parameter specifies the index of the new axis in the dimensions of the result.
     * For example, if `axis=0` it will be the first dimension and if `axis=-1` it will be the last
     * dimension.
     *
     * @param arrays input {@code NDArray}[]. each {@code NDArray} must have the same shape.
     * @param axis the axis in the result array along which the input arrays are stacked.
     * @return {@code NDArray}. The stacked array has one more dimension than the input arrays.
     */
    NDArray stack(NDArray[] arrays, int axis);

    /**
     * Joins a sequence of {@code NDArray} along axis 0.
     *
     * @param arrays input {@code NDArray}[]. each {@code NDArray} must have the same shape.
     * @return {@code NDArray}. The stacked array has one more dimension than the input arrays.
     */
    default NDArray stack(NDArray[] arrays) {
        return stack(arrays, 0);
    }

    /**
     * Joins a sequence of {@code NDArray} in NDList along a new axis.
     *
     * <p>The axis parameter specifies the index of the new axis in the dimensions of the result.
     * For example, if `axis=0` it will be the first dimension and if `axis=-1` it will be the last
     * dimension.
     *
     * @param arrays input NDList. each {@code NDArray} must have the same shape.
     * @param axis the axis in the result array along which the input arrays are stacked.
     * @return {@code NDArray}. The stacked array has one more dimension than the input arrays.
     */
    NDArray stack(NDList arrays, int axis);

    /**
     * Joins a sequence of {@code NDArray} in NDList along axis 0.
     *
     * @param arrays input NDList. each {@code NDArray} must have the same shape.
     * @return {@code NDArray}. The stacked array has one more dimension than the input arrays.
     */
    default NDArray stack(NDList arrays) {
        return stack(arrays, 0);
    }

    /**
     * Joins a {@code NDArray} along a new axis.
     *
     * @param array input {@code NDArray} and it must have the same shape with {@code NDArray} that
     *     invoke the function.
     * @param axis the axis in the result array along which the input arrays are stacked.
     * @return {@code NDArray}. The stacked array has one more dimension than the input arrays.
     */
    default NDArray stack(NDArray array, int axis) {
        return stack(new NDArray[] {array}, axis);
    }

    /**
     * Joins a {@code NDArray} along axis 0.
     *
     * @param array input {@code NDArray} and it must have the same shape with {@code NDArray} that
     *     invoke the function.
     * @return {@code NDArray}. The stacked array has one more dimension than the input arrays.
     */
    default NDArray stack(NDArray array) {
        return stack(new NDArray[] {array}, 0);
    }

    /**
     * Joins a sequence of {@code NDArray} along an existing axis.
     *
     * @param arrays input {@code NDArray}[] must have the same shape, except in the dimension
     *     corresponding to `axis` (the first, by default).
     * @param axis the axis along which the arrays will be joined.
     * @return the concatenated {@code NDArray}
     */
    NDArray concat(NDArray[] arrays, int axis);

    /**
     * Joins a sequence of {@code NDArray} along axis 0.
     *
     * @param arrays input {@code NDArray}[] in NDList must have the same shape, except in the
     *     dimension corresponding to `axis` (the first, by default).
     * @return the concatenated {@code NDArray}
     */
    default NDArray concat(NDArray[] arrays) {
        return concat(arrays, 0);
    }

    /**
     * Joins a NDList along an existing axis.
     *
     * @param arrays input input {@code NDArray} in NDList must have the same shape, except in the
     *     dimension corresponding to `axis` (the first, by default).
     * @param axis the axis along which the arrays will be joined.
     * @return the concatenated {@code NDArray}
     */
    default NDArray concat(NDList arrays, int axis) {
        return concat(arrays.toArray(), axis);
    }

    /**
     * Joins a NDList along axis 0.
     *
     * @param arrays input {@code NDArray} in NDList must have the same shape, except in the
     *     dimension corresponding to `axis` (the first, by default).
     * @return the concatenated {@code NDArray}
     */
    default NDArray concat(NDList arrays) {
        return concat(arrays, 0);
    }

    /**
     * Joins a {@code NDArray} along an existing axis.
     *
     * @param array the {@code NDArray} must have the same shape, except in the dimension
     *     corresponding to `axis` (the first, by default).
     * @param axis the axis along which the arrays will be joined.
     * @return the concatenated {@code NDArray}
     */
    default NDArray concat(NDArray array, int axis) {
        return concat(new NDArray[] {array}, axis);
    }

    /**
     * Joins a {@code NDArray} along axis 0.
     *
     * @param array the {@code NDArray} must have the same shape, except in the dimension
     *     corresponding to `axis` (the first, by default).
     * @return the concatenated {@code NDArray}
     */
    default NDArray concat(NDArray array) {
        return concat(new NDArray[] {array}, 0);
    }

    /**
     * Clips (limit) the values in an array.
     *
     * <p>Given an interval, values outside the interval are clipped to the interval edges. For
     * example, if an interval of ``[0, 1]`` is specified, values smaller than 0 become 0, and
     * values larger than 1 become 1.
     *
     * @param min minimum value double type.
     * @param max maximum value double type.
     * @return an {@code NDArray} with the elements of `a`, but where values &lt; `min` are replaced
     *     with `min`, and those &gt; `max` with `max`.
     */
    NDArray clip(double min, double max);

    /**
     * Clips (limit) the values in an array.
     *
     * <p>Given an interval, values outside the interval are clipped to the interval edges. For
     * example, if an interval of ``[0, 1]`` is specified, values smaller than 0 become 0, and
     * values larger than 1 become 1.
     *
     * @param min minimum value int type.
     * @param max maximum value int type.
     * @return an {@code NDArray} with the elements of `a`, but where values &lt; `min` are replaced
     *     with `min`, and those &gt; `max` with `max`.
     */
    default NDArray clip(int min, int max) {
        return clip((double) min, (double) max);
    }

    /**
     * Transposes one dimension with another one.
     *
     * @param dimension the dimension to swap
     * @param with the one to swap it with
     * @return the swapped axes view
     */
    default NDArray swapAxes(int dimension, int with) {
        int[] dims = IntStream.range(0, getShape().dimension()).toArray();
        int tmp = dims[dimension];
        dims[dimension] = dims[with];
        dims[with] = tmp;
        return transpose(dims);
    }

    /**
     * Reverses the order of the dimensions in the {@code NDArray}.
     *
     * @return the newly permuted array
     */
    NDArray transpose();

    /**
     * Reorders the dimensions in the {@code NDArray}.
     *
     * @param dimensions the dimensions to swap to
     * @return the newly permuted array
     * @throws IllegalArgumentException thrown when passing a dimension that is greater than the
     *     actual number of dimensions
     */
    NDArray transpose(int[] dimensions);

    /**
     * Returns the size along a specified dimension.
     *
     * @param dimension the dimension to return the size for
     * @return the size of the array along the specified dimension
     */
    default long size(int dimension) {
        return getShape().size(dimension);
    }

    /**
     * Returns the total number of elements in the {@code NDArray}.
     *
     * @return the number of elements in the NDArray
     */
    default long size() {
        return getShape().size();
    }

    /**
     * Broadcasts this NDArray to be the specified shape.
     *
     * @param shape the new shape of this NDArray
     * @return the broadcasted NDArray
     */
    NDArray broadcast(long... shape);

    /**
     * Broadcasts this NDArray to be the specified shape.
     *
     * @param result the result array
     * @return the broadcasted {@code NDArray}
     */
    NDArray broadcast(NDArray result);

    /**
     * This method checks 2 NDArrays equality with given eps.
     *
     * @param o object to compare with
     * @param eps Epsilon value to use for the quality operation
     * @return the result {@code NDArray}
     */
    boolean equalsWithEps(Object o, double eps);

    /**
     * Checks 2 NDArrays for equal shapes.
     *
     * <pre>
     * Shapes are considered equal if:
     * (a) Both arrays have equal rank, and
     * (b) size(0)...size(rank()-1) are equal for both arrays
     * </pre>
     *
     * @param other other
     * @return {@code true} if shape are the same
     */
    boolean equalShapes(NDArray other);

    /**
     * Return element-wise remainder of division.
     *
     * <p>NDArray nd = factory.create(new float[] {-3, -5}, null, new Shape(2)); nd.mod(-2) //
     * return [-1, -1]
     *
     * @param n divisor number
     * @return copy of {@code NDArray} after division
     */
    NDArray mod(Number n);

    /**
     * Return element-wise remainder of division.
     *
     * @param n divisor number
     * @return Copy of {@code NDArray} after division
     */
    NDArray modi(Number n);

    /**
     * Copy element-wise remainder of division.
     *
     * @param other the second NDArray to divide
     * @return the result of the divide
     */
    NDArray mod(NDArray other);

    /**
     * In place element-wise remainder of division.
     *
     * @param other the second NDArray to divide
     * @return the result of the divide
     */
    NDArray modi(NDArray other);

    /**
     * This method returns index of highest value along specified axi(e)s.
     *
     * @param axis the axis along which to find argmax
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return Array containing indices
     */
    NDArray argMax(int axis, boolean keepDims);

    /**
     * This method returns index of lowest value.
     *
     * @return Array containing indices
     */
    NDArray argMin();

    /**
     * This method returns index of lowest value along specified axi(e)s.
     *
     * @param axis the axis along which to find argmax
     * @param keepDims {@code true} to keep the specified axes as size 1 in the output array, {@code
     *     false} to squeeze the values out of the output array
     * @return Array containing indices
     */
    NDArray argMin(int axis, boolean keepDims);

    /**
     * This method returns index of highest value.
     *
     * @return Array containing indices
     */
    NDArray argMax();

    /**
     * Returns percentile value for this {@code NDArray}.
     *
     * @param percentile target percentile in range of 0..100
     * @return the result {@code NDArray} NDArray
     */
    Number percentileNumber(Number percentile);

    /**
     * Returns median value for this {@code NDArray}.
     *
     * @return Median value for array
     */
    Number medianNumber();

    /**
     * Returns median along given dimension(s).
     *
     * @param dimension Dimension along which to perform the median operation
     * @return Median along specified dimensions
     */
    NDArray median(int... dimension);

    /**
     * Returns median along given dimension(s).
     *
     * @param percentile target percentile in range of 0..100
     * @param dimension Dimension to calculate percentile for
     * @return the result {@code NDArray} NDArray
     */
    NDArray percentile(Number percentile, int... dimension);

    // ------------ Sparse methods ------------

    /**
     * Returns a dense representation of the sparse {@code NDArray}.
     *
     * @return the result {@code NDArray} NDArray
     */
    NDArray toDense();

    /**
     * Returns the number of non-null element.
     *
     * @return nnz
     */
    long nonzero();

    /**
     * Returns {@code true} if this NDArray is special case: no-value {@code NDArray}.
     *
     * @return {@code true} if this NDArray is empty
     */
    boolean isEmpty();

    /**
     * Returns {@code true} if all elements within this array are non-zero or {@code true}.
     *
     * @return {@code true} if all elements within this array are non-zero or {@code true}
     */
    default boolean all() {
        return nonzero() == size();
    }

    /**
     * Returns {@code true} if any of the elements within this array are non-zero or {@code true }.
     *
     * @return {@code true} if any of the elements within this array are non-zero or {@code true }
     */
    default boolean any() {
        return nonzero() > 0;
    }

    /**
     * Returns {@code true} if none of the elements within this array are non-zero or {@code true }.
     *
     * @return {@code true} if none of the elements within this array are non-zero or {@code true }
     */
    default boolean none() {
        return nonzero() == 0;
    }

    /**
     * Computes the truth value of NOT x element-wise.
     *
     * @return the result {@code NDArray}
     */
    NDArray logicalNot();

    ////////////////////////////////////////
    // basic numeric operations
    ////////////////////////////////////////

    /**
     * Calculates the absolute value element-wise.
     *
     * @return the result {@code NDArray}
     */
    NDArray abs();

    /**
     * Returns the element-wise square of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray square();

    /**
     * Returns the cube-root of an array, element-wise.
     *
     * @return the result {@code NDArray}
     */
    NDArray cbrt();

    /**
     * Returns the floor of the input, element-wise. The floor of the scalar x is the largest
     * integer i, such that i &lt;= x. It is often denoted as \lfloor x \rfloor.
     *
     * @return the result {@code NDArray}
     */
    NDArray floor();

    /**
     * Returns the ceiling of the input, element-wise. The ceil of the scalar x is the smallest
     * integer i, such that i &gt;= x. It is often denoted as \lceil x \rceil.
     *
     * @return the result {@code NDArray}
     */
    NDArray ceil();

    /**
     * Returns element-wise rounded value to the nearest integer of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray round();

    /**
     * Returns the element-wise truncated value of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray trunc();

    /**
     * Returns element-wise exponential value of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray exp();

    /**
     * Raises the power of each element in the {@code NDArray}.
     *
     * @param n the number to raise the power to
     * @return the result {@code NDArray}
     */
    NDArray pow(Number n);

    /**
     * Raises the power of each element in the ndarray in-place.
     *
     * @param n the number to raise the power to
     * @return the result {@code NDArray}
     */
    NDArray powi(Number n);

    /**
     * Raises the power of each element in the ndarray by the corresponding element in the other
     * {@code NDArray}.
     *
     * @param other the ndarray by which the raise the power by
     * @return the result {@code NDArray}
     */
    NDArray pow(NDArray other);

    /**
     * Raises the power of each element in the ndarray by the corresponding element in the other
     * ndarray in-place.
     *
     * @param other the ndarray by which the raise the power by
     * @return the result {@code NDArray}
     */
    NDArray powi(NDArray other);

    /**
     * Returns element-wise Natural logarithmic value of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray log();

    /**
     * Returns element-wise Base-2 logarithmic value of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray log10();

    /**
     * Returns element-wise Base-2 logarithmic value of the input.
     *
     * @return the result {@code NDArray}
     */
    NDArray log2();

    /**
     * Computes the element-wise sine of the input array. The input should be in radians ( 2𝜋 rad
     * equals 360 degrees).
     *
     * @return the result {@code NDArray}
     */
    NDArray sin();

    /**
     * Computes the element-wise cosine of the input array. The input should be in radians ( 2𝜋 rad
     * equals 360 degrees).
     *
     * @return the result {@code NDArray}
     */
    NDArray cos();

    /**
     * Computes the element-wise tangent of the input array. The input should be in radians ( 2𝜋
     * rad equals 360 degrees).
     *
     * @return the result {@code NDArray}
     */
    NDArray tan();

    /**
     * Returns element-wise inverse sine of the input array. The input should be in the range [-1,
     * 1]. The output is in the closed interval of [ −𝜋/2 , 𝜋/2 ].
     *
     * @return the result {@code NDArray}
     */
    NDArray asin();

    /**
     * Returns element-wise inverse cosine of the input array. The input should be in the range [-1,
     * 1]. The output is in the closed interval of [ −𝜋/2 , 𝜋/2 ].
     *
     * @return the result {@code NDArray}
     */
    NDArray acos();

    /**
     * Returns element-wise inverse tangent of the input array. The input should be in the range
     * [-1, 1]. The output is in the closed interval of [ −𝜋/2 , 𝜋/2 ].
     *
     * @return the result {@code NDArray}
     */
    NDArray atan();

    /**
     * Converts each element of the input array from radians to degrees.
     * 𝑑𝑒𝑔𝑟𝑒𝑒𝑠([0,𝜋/2,𝜋,3𝜋/2,2𝜋])=[0,90,180,270,360].
     *
     * @return the result {@code NDArray}
     */
    NDArray toDegrees();

    /**
     * Converts each element of the input array from degrees to radians.
     * 𝑟𝑎𝑑𝑖𝑎𝑛𝑠([0,90,180,270,360])=[0,𝜋/2,𝜋,3𝜋/2,2𝜋]
     *
     * @return the result {@code NDArray}
     */
    NDArray toRadians();

    /**
     * Returns the hyperbolic sine of the input array, computed element-wise.
     * 𝑠𝑖𝑛ℎ(𝑥)=0.5×(𝑒𝑥𝑝(𝑥)−𝑒𝑥𝑝(−𝑥))
     *
     * @return the result {@code NDArray}
     */
    NDArray sinh();

    /**
     * Returns the hyperbolic cosine of the input array, computed element-wise.
     * 𝑐𝑜𝑠ℎ(𝑥)=0.5×(𝑒𝑥𝑝(𝑥)+𝑒𝑥𝑝(−𝑥))
     *
     * @return the result {@code NDArray}
     */
    NDArray cosh();

    /**
     * Returns the hyperbolic tangent of the input array, computed element-wise.
     * 𝑡𝑎𝑛ℎ(𝑥)=𝑠𝑖𝑛ℎ(𝑥)/𝑐𝑜𝑠ℎ(𝑥)
     *
     * @return the result {@code NDArray}
     */
    NDArray tanh();

    /**
     * Returns the element-wise inverse hyperbolic sine of the input array, computed element-wise.
     *
     * @return the result {@code NDArray}
     */
    NDArray asinh();

    /**
     * Returns the element-wise inverse hyperbolic cosine of the input array, computed element-wise.
     *
     * @return the result {@code NDArray}
     */
    NDArray acosh();

    /**
     * Returns the element-wise inverse hyperbolic tangent of the input array, computed
     * element-wise.
     *
     * @return the result {@code NDArray}
     */
    NDArray atanh();

    /**
     * Returns an internal representative of Native {@code NDArray}.
     *
     * <p>This method should only be used by Engine provider.
     *
     * @return an internal representative of Native NDArray
     */
    NDArrayEx getNDArrayInternal();

    @Override
    void close();
}