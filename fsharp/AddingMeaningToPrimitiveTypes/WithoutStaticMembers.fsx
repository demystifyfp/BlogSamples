type IncomeSource =
| Salary
| Royalty

type ExpenseCategory =
| Food
| Entertainment

type Money = Money of decimal

type Income = {
  Amount : Money
  Source : IncomeSource
} 

type Expense = {
  Amount : Money
  Category : ExpenseCategory
}

type Transaction =
| Credit of Income 
| Debit of Expense



// Transaction list -> Money
let balance transactions =
  transactions
  |> List.map (
    function
    | Credit x -> 
      let (Money m) = x.Amount 
      m
    | Debit y ->
      let (Money m) = y.Amount
      -m
  )
  |> List.sum
  |> Money




let rec getExpenses transactions =
  getExpenses' transactions []
and getExpenses' transactions expenses =
  match transactions with
  | [] -> expenses
  | x :: xs -> 
    match x with
    | Debit expense ->
      (expense :: expenses)
      |> getExpenses' xs
    | _ -> getExpenses' xs expenses

let getExpenditure expenseCategory transactions =
  getExpenses transactions
  |> List.filter (fun e -> e.Category = expenseCategory)
  |> List.sumBy (fun expense -> 
    let (Money m) = expense.Amount 
    m
  )
  |> Money


// ExpenseCategory -> Transaction list list -> Money
let averageExpenditure expenseCategory transactionsList =
  transactionsList
  |> List.map (getExpenditure expenseCategory)
  |> List.map (fun (Money m) -> m)
  |> List.average
  |> Money


let transactions = 
  
  [ {Category = Food; Amount = Money 10m}
    {Category = Food; Amount = Money 15m}
  ] |> List.map Debit


let transactions2 = 
  
  [ {Category = Food; Amount = Money 5m}
    {Category = Food; Amount = Money 10m}
  ] |> List.map Debit


averageExpenditure Food [transactions;transactions2]
