open System
type IncomeSource =
| Salary
| Royalty

type ExpenseCategory =
| Food
| Entertainment

type Money = Money of decimal with
  static member (+) (Money m1, Money m2) = Money (m1 + m2)
  static member get_Zero() = Money 0m
  static member DivideByInt ((Money m), (x : int)) = 
    Decimal.Divide(m, Convert.ToDecimal(x))
    |> Money
    

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


let rec private getExpenses' expenseCategory transactions expenses =
  match transactions with
  | [] -> expenses
  | x :: xs -> 
    match x with
    | Debit expense when expense.Category = expenseCategory ->
      (expense :: expenses)
      |> getExpenses' expenseCategory xs
    | _ -> getExpenses' expenseCategory xs expenses

let getExpenses expenseCategory transactions =
  getExpenses' expenseCategory transactions []

let getExpenditure expenseCategory transactions =
  getExpenses expenseCategory transactions
  |> List.sumBy (fun expense -> expense.Amount)

// ExpenseCategory -> Transaction list list -> Money
let averageExpenditure expenseCategory transactionsList =
  transactionsList
  |> List.map (getExpenditure expenseCategory)
  |> List.average

let transactions = 
  
  [ {Category = Food; Amount = Money 10m}
    {Category = Food; Amount = Money 15m}
  ] |> List.map Debit


let transactions2 = 
  
  [ {Category = Food; Amount = Money 5m}
    {Category = Food; Amount = Money 10m}
  ] |> List.map Debit


averageExpenditure Food [transactions;transactions2]