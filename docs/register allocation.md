[Standford cs143](https://web.stanford.edu/class/archive/cs/cs143/cs143.1128/lectures/17/Slides17.pdf)


The live range for a variable is the set of program points at which that variable is live.
The live interval for a variable is the smallest subrange of the IR code containing all a variable's live ranges.

linear scan register allocation:
  very efficient
  disadvantage: use live intervals rather than live ranges


Second-Chance Bin Packing:
	A more aggressive version of linear-scan. 
	Uses live ranges instead of live intervals.
	If a variable must be spilled, don't spill all uses of it.

register interference graph (RIG):

- Each node represents a temporary value
- Each edge (u, v) indicates pair of temporaries that cannot be assigned to the same register. 

When to add an interference edge:

- u, v are live at the same time
- we can not produce result at some certain register

We need to solve the K-coloring problem on the interference graph.

We wish to coalesce only where it is safe to do so, that is, where the coalescing will not render the graph uncolorable. Both of the following strategies are safe:

- Briggs: Nodes a and b can be coalesced if the resulting node ab will have fewer than K neighbors of significant degree
- George: Nodes a and b can be coalesced if, for every neighbor t of a, either t already interferes with b or t is of insignificant degree. 





