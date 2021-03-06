/**
 * UNCLASSIFIED
 *
 * Copyright 2020 Northrop Grumman Systems Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ngc.seaside.jellyfish.cli.command.analyzebudget;

import com.ngc.seaside.jellyfish.cli.command.analyzebudget.budget.Budget;
import com.ngc.seaside.jellyfish.cli.command.analyzebudget.budget.BudgetResult;
import com.ngc.seaside.jellyfish.cli.command.analyzebudget.budget.SdBudgetAdapter;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IModelReferenceField;

import tec.uom.se.quantity.Quantities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

import javax.measure.Quantity;

/**
 * Finds the budgets in a model and its nested parts. For each budget, computes the actual sum of the budget, and
 * returns budget results for each budget.
 */
public class BudgetAnalysisTask extends RecursiveTask<Set<BudgetResult<? extends Quantity<?>>>> {

   private IModel model;
   private SdBudgetAdapter adapter;

   public BudgetAnalysisTask(IModel model, SdBudgetAdapter adapter) {
      this.model = model;
      this.adapter = adapter;
   }

   @Override
   protected Set<BudgetResult<? extends Quantity<?>>> compute() {
      Set<BudgetResult<? extends Quantity<?>>> results = new LinkedHashSet<>();

      Set<Budget<? extends Quantity<?>>> budgets = adapter.getBudgets(model);
      List<BudgetSumTask<? extends Quantity<?>>> subtasks = new ArrayList<>(budgets.size());
      for (Budget<? extends Quantity<?>> budget : budgets) {
         BudgetSumTask<? extends Quantity<?>> subtask = new BudgetSumTask<>(model, budget);
         subtask.fork();
         subtasks.add(subtask);
      }
      for (BudgetSumTask<? extends Quantity<?>> subtask : subtasks) {
         results.add(subtask.getResult());
      }

      Collection<IModelReferenceField> parts = model.getParts();
      List<BudgetAnalysisTask> analysisTasks = new ArrayList<>(parts.size());
      for (IModelReferenceField part : parts) {
         BudgetAnalysisTask subtask = new BudgetAnalysisTask(part.getType(), adapter);
         subtask.fork();
         analysisTasks.add(subtask);
      }
      for (BudgetAnalysisTask subtask : analysisTasks) {
         Set<BudgetResult<? extends Quantity<?>>> subResults = subtask.join();
         results.addAll(subResults);
      }

      return results;
   }

   /**
    * Recursively calculates the sum for the supplied budget.
    * 
    * @param <T> unit type
    */
   private class BudgetSumTask<T extends Quantity<T>> extends RecursiveTask<Quantity<T>> {

      private IModel model;
      private Budget<T> budget;

      private BudgetSumTask(IModel model, Budget<T> budget) {
         this.model = model;
         this.budget = budget;
      }

      private BudgetResult<T> getResult() {
         Quantity<T> quantity = this.join();
         return new BudgetResult<>(budget, quantity);
      }

      @Override
      protected Quantity<T> compute() {
         Quantity<T> quantity =
                  adapter.getBudgetValue(model, budget).orElseGet(() -> Quantities.getQuantity(0, budget.getUnit()));
         Collection<IModelReferenceField> parts = model.getParts();
         List<BudgetSumTask<T>> subtasks = new ArrayList<>(parts.size());
         for (IModelReferenceField part : parts) {
            BudgetSumTask<T> subtask = new BudgetSumTask<>(part.getType(), budget);
            subtask.fork();
            subtasks.add(subtask);
         }

         for (BudgetSumTask<T> subtask : subtasks) {
            Quantity<T> subquantity = subtask.join();
            quantity = quantity.add(subquantity);
         }
         return quantity;
      }

   }
}
