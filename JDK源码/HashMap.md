方法：
HashMap
HashMap
HashMap
HashMap
HashMap
afterNodeAccess
afterNodeInsertion
afterNodeRemoval
capacity
clear
clone
comparableClassFor
compareComparables
compute
computeIfAbsent
computeIfPresent
containsKey
containsValue
entrySet
forEach
get
getNode
getOrDefault
hash
internalWriteEntries
isEmpty
keySet
loadFactor
merge
newNode
newTreeNode
put
putAll
putIfAbsent
putMapEntries
putVal
readObject
reinitialize
remove
remove
removeNode
replace
replace
replaceAll
replacementNode
replacementTreeNode
resize
size
tableSizeFor
treeifyBin
values
writeObject

属性：
DEFAULT_INITIAL_CAPACITY
DEFAULT_LOAD_FACTOR
entrySet
loadFactor
MAXIMUM_CAPACITY
MIN_TREEIFY_CAPACITY
modCount
serialVersionUID
size
table
threshold
TREEIFY_THRESHOLD
UNTREEIFY_THRESHOLD

内部类
EntryIterator
EntrySet
EntrySpliterator
HashIterator
HashMapSpliterator
KeyIterator
KeySet
KeySpliterator
Node
TreeNode
ValueIterator
Values
ValueSpliterator





**transient int size：**表示当前HashMap包含的键值对数量

**transient int modCount：**表示当前HashMap修改次数

**int threshold：**表示当前HashMap能够承受的最多的键值对数量，一旦超过这个数量HashMap就会进行扩容

**final float loadFactor：**负载因子，用于扩容

**static final int DEFAULT_INITIAL_CAPACITY = 1 << 4：**默认的table初始容量

**static final float DEFAULT_LOAD_FACTOR = 0.75f：**默认的负载因子

**static final int TREEIFY_THRESHOLD = 8:** 链表长度大于该参数转红黑树

**static final int UNTREEIFY_THRESHOLD = 6:** 当树的节点数小于等于该参数转成链表